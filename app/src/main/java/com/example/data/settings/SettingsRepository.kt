package com.example.data.settings

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.example.BuildConfig
import com.example.data.RssFeedItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/** Everything the user configures, read as one consistent snapshot. */
data class UserSettings(
    val keywords: List<String> = SettingsRepository.DEFAULT_KEYWORDS,
    val rssFeeds: List<RssFeedItem> = emptyList(),
    val youtubeChannelIds: String = "",
    val visitedLinks: Set<String> = emptySet(),
    val isDarkMode: Boolean = true,
    /** Small "new items" counts on the bottom bar (home, video); can be turned off in settings. */
    val showNewBadges: Boolean = true,
    /** When the home / video tab was last looked at (epoch millis; 0 = never). */
    val lastSeenHomeAt: Long = 0L,
    val lastSeenVideoAt: Long = 0L
)

/**
 * User settings stored in Jetpack DataStore (asynchronous, transactional, never on the main thread).
 *
 * Two stores:
 *  - "settings": keywords, feeds, visited links, theme. Migrated once from the old `keywords_prefs`.
 *  - "secret_settings": the YouTube API key. Excluded from cloud backup and device transfer
 *    (see res/xml backup rules). Migrated once from the old `secret_prefs`.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @param:SettingsStore private val store: DataStore<Preferences>,
    @param:SecretStore private val secretStore: DataStore<Preferences>
) {

    val settings: Flow<UserSettings> = store.data.map { prefs ->
        UserSettings(
            keywords = prefs[KEYWORDS]?.takeIf { it.isNotEmpty() }?.toList() ?: DEFAULT_KEYWORDS,
            rssFeeds = parseFeeds(prefs[RSS_FEEDS_JSON]),
            youtubeChannelIds = cleanChannelIds(prefs[YOUTUBE_CHANNEL_ID]),
            visitedLinks = visitedLinksOrdered(prefs).toSet(),
            isDarkMode = prefs[DARK_MODE] ?: true,
            showNewBadges = prefs[SHOW_NEW_BADGES] ?: true,
            lastSeenHomeAt = prefs[LAST_SEEN_HOME] ?: 0L,
            lastSeenVideoAt = prefs[LAST_SEEN_VIDEO] ?: 0L
        )
    }.distinctUntilChanged()

    /** User-entered key, else the build-time key from .env, else blank. */
    val youtubeApiKey: Flow<String> = secretStore.data.map { prefs ->
        prefs[YOUTUBE_API_KEY]?.takeIf { it.isNotBlank() }
            ?: BuildConfig.YOUTUBE_API_KEY.takeIf { it != "MY_YOUTUBE_API_KEY" }
            ?: ""
    }.distinctUntilChanged()

    suspend fun current(): UserSettings = settings.first()

    suspend fun addKeyword(keyword: String) {
        val clean = keyword.trim()
        if (clean.isBlank()) return
        store.edit { prefs ->
            val current = prefs[KEYWORDS]?.takeIf { it.isNotEmpty() } ?: DEFAULT_KEYWORDS.toSet()
            prefs[KEYWORDS] = current + clean
        }
    }

    suspend fun removeKeyword(keyword: String) {
        store.edit { prefs ->
            val current = prefs[KEYWORDS]?.takeIf { it.isNotEmpty() } ?: DEFAULT_KEYWORDS.toSet()
            prefs[KEYWORDS] = current - keyword
        }
    }

    suspend fun addRssFeed(title: String, rawUrl: String) {
        var url = rawUrl.trim()
        if (url.isBlank()) return
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            url = "https://$url"
        }
        store.edit { prefs ->
            val current = parseFeeds(prefs[RSS_FEEDS_JSON])
            if (current.any { it.url.equals(url, ignoreCase = true) }) return@edit
            val feed = RssFeedItem(title = title.trim().ifBlank { domainOf(url) }, url = url)
            prefs[RSS_FEEDS_JSON] = feedsToJson(current + feed)
        }
    }

    suspend fun removeRssFeed(url: String) {
        store.edit { prefs ->
            val remaining = parseFeeds(prefs[RSS_FEEDS_JSON]).filterNot { it.url.equals(url.trim(), ignoreCase = true) }
            prefs[RSS_FEEDS_JSON] = feedsToJson(remaining)
        }
    }

    suspend fun setYoutubeApiKey(key: String) {
        secretStore.edit { it[YOUTUBE_API_KEY] = key.trim() }
    }

    suspend fun setYoutubeChannelIds(ids: String) {
        store.edit { it[YOUTUBE_CHANNEL_ID] = ids.trim() }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        store.edit { it[DARK_MODE] = enabled }
    }

    suspend fun setShowNewBadges(enabled: Boolean) {
        store.edit { it[SHOW_NEW_BADGES] = enabled }
    }

    /** Remembers that the home tab was just looked at, so its "new" count starts again from zero. */
    suspend fun markHomeSeen(at: Long = System.currentTimeMillis()) {
        store.edit { it[LAST_SEEN_HOME] = at }
    }

    suspend fun markVideoSeen(at: Long = System.currentTimeMillis()) {
        store.edit { it[LAST_SEEN_VIDEO] = at }
    }

    /** When news was last fully refreshed (epoch millis), so a quick re-open doesn't refetch everything. */
    suspend fun lastSyncAt(): Long = store.data.first()[LAST_SYNC_AT] ?: 0L

    suspend fun setLastSyncAt(millis: Long) {
        store.edit { it[LAST_SYNC_AT] = millis }
    }

    /** Version of the country rules that the stored `region` values were computed with. */
    suspend fun regionRulesVersion(): Int = store.data.first()[REGION_RULES_VERSION] ?: 0

    suspend fun setRegionRulesVersion(version: Int) {
        store.edit { it[REGION_RULES_VERSION] = version }
    }

    /** Remembers [link] as read, keeping the newest [MAX_VISITED] and dropping the oldest first. */
    suspend fun markLinkVisited(link: String) {
        if (link.isBlank()) return
        store.edit { prefs ->
            val ordered = visitedLinksOrdered(prefs).toMutableList()
            ordered.remove(link)
            ordered.add(link)
            val trimmed = if (ordered.size > MAX_VISITED) ordered.takeLast(TRIM_VISITED_TO) else ordered
            prefs[VISITED_LINKS_JSON] = JSONArray(trimmed).toString()
            prefs.remove(LEGACY_VISITED_SET)
        }
    }

    companion object {
        val DEFAULT_KEYWORDS = listOf(
            "বিজিবি", "বর্ডার গার্ড বাংলাদেশ", "BGB", "বিএসএফ", "BSF",
            "সীমান্ত", "বাংলাদেশ ভারত সীমান্ত", "সীমান্ত সংবাদ"
        )
        private const val MAX_VISITED = 1_500
        private const val TRIM_VISITED_TO = 1_200
        private const val LEGACY_PLACEHOLDER_CHANNEL = "UCxAI1M5Wv1rE4XbT1gNnt7g"

        // Key names match the old SharedPreferences so SharedPreferencesMigration carries values over as-is
        internal val KEYWORDS = stringSetPreferencesKey("keywords_set")
        internal val RSS_FEEDS_JSON = stringPreferencesKey("custom_rss_feeds_json")
        internal val YOUTUBE_CHANNEL_ID = stringPreferencesKey("youtube_channel_id")
        internal val VISITED_LINKS_JSON = stringPreferencesKey("visited_links_json")
        internal val LEGACY_VISITED_SET = stringSetPreferencesKey("visited_links_set")
        internal val DARK_MODE = booleanPreferencesKey("dark_mode")
        internal val YOUTUBE_API_KEY = stringPreferencesKey("youtube_api_key")
        internal val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
        internal val REGION_RULES_VERSION = intPreferencesKey("region_rules_version")
        internal val SHOW_NEW_BADGES = booleanPreferencesKey("show_new_badges")
        internal val LAST_SEEN_HOME = longPreferencesKey("last_seen_home_at")
        internal val LAST_SEEN_VIDEO = longPreferencesKey("last_seen_video_at")

        internal const val LEGACY_PREFS = "keywords_prefs"
        internal const val LEGACY_SECRET_PREFS = "secret_prefs"

        private fun visitedLinksOrdered(prefs: Preferences): List<String> {
            val raw = prefs[VISITED_LINKS_JSON] ?: return prefs[LEGACY_VISITED_SET].orEmpty().toList()
            return runCatching {
                val arr = JSONArray(raw)
                List(arr.length()) { arr.getString(it) }
            }.getOrDefault(emptyList())
        }

        private fun cleanChannelIds(raw: String?): String {
            if (raw.isNullOrBlank() || raw.contains(LEGACY_PLACEHOLDER_CHANNEL)) return ""
            return raw.trim()
        }

        private fun parseFeeds(raw: String?): List<RssFeedItem> {
            if (raw.isNullOrBlank()) return emptyList()
            return runCatching {
                val arr = JSONArray(raw)
                (0 until arr.length()).mapNotNull { i ->
                    val obj = arr.getJSONObject(i)
                    val url = obj.optString("url", "").trim()
                    if (url.isBlank()) null
                    else RssFeedItem(title = obj.optString("title", "").trim().ifBlank { domainOf(url) }, url = url)
                }
            }.getOrDefault(emptyList())
        }

        private fun feedsToJson(feeds: List<RssFeedItem>): String {
            val arr = JSONArray()
            feeds.forEach { arr.put(JSONObject().put("title", it.title).put("url", it.url)) }
            return arr.toString()
        }

        private fun domainOf(url: String): String = runCatching {
            (java.net.URI(url).host ?: url).removePrefix("www.").removePrefix("feeds.").removePrefix("rss.")
        }.getOrDefault(url)

        /**
         * The main store, with a one-time migration from the old `keywords_prefs` file. The API key
         * is deliberately not migrated here; [createSecretStore] moves it out of the backed-up file.
         */
        fun createSettingsStore(
            context: Context,
            scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            fileName: String = "settings"
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
            migrations = listOf(
                SharedPreferencesMigration(
                    context,
                    LEGACY_PREFS,
                    keysToMigrate = setOf(
                        "keywords_set", "custom_rss_feeds_json", "youtube_channel_id",
                        "visited_links_json", "visited_links_set"
                    )
                )
            ),
            scope = scope,
            produceFile = { context.preferencesDataStoreFile(fileName) }
        )

        /** The API-key store (excluded from backup), migrated from `secret_prefs` and legacy `keywords_prefs`. */
        fun createSecretStore(
            context: Context,
            scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            fileName: String = "secret_settings"
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
            migrations = listOf(
                SharedPreferencesMigration(context, LEGACY_SECRET_PREFS),
                LegacyApiKeyMigration(context)
            ),
            scope = scope,
            produceFile = { context.preferencesDataStoreFile(fileName) }
        )
    }
}

/** Very old versions kept the API key in the backed-up `keywords_prefs`; move it to the secret store. */
private class LegacyApiKeyMigration(context: Context) : DataMigration<Preferences> {
    private val legacy = context.getSharedPreferences(SettingsRepository.LEGACY_PREFS, Context.MODE_PRIVATE)

    override suspend fun shouldMigrate(currentData: Preferences): Boolean = legacy.contains("youtube_api_key")

    override suspend fun migrate(currentData: Preferences): Preferences {
        val key = legacy.getString("youtube_api_key", null)
        val mutable: MutablePreferences = currentData.toMutablePreferences()
        if (!key.isNullOrBlank() && mutable[SettingsRepository.YOUTUBE_API_KEY].isNullOrBlank()) {
            mutable[SettingsRepository.YOUTUBE_API_KEY] = key
        }
        return mutable.toPreferences()
    }

    override suspend fun cleanUp() {
        legacy.edit().remove("youtube_api_key").apply()
    }
}
