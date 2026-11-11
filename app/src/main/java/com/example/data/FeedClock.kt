package com.example.data

import android.content.Context
import android.content.SharedPreferences
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap

/**
 * Repairs publish times of sources that state the wrong time zone. Both cases below were measured
 * against the publishers' own pages on 2026-11-10:
 *
 *  - Bing News RSS writes US Pacific wall-clock time but labels it "GMT". Every Bing story therefore
 *    looked 8 hours (7 in summer) older than it was and sank far down the newest-first list, which
 *    made it seem that Bing had stopped delivering news.
 *  - Several Bangladeshi sites write local time under a "UTC"/"GMT"/"+0000" label. Their stories
 *    looked 6 hours in the future, were rejected as "not published yet" and showed up 6 hours late.
 *
 * Both repairs undo themselves when the source fixes its feed (see each function).
 */
object FeedClock {

    private val PACIFIC: TimeZone = TimeZone.getTimeZone("America/Los_Angeles")

    /** A few minutes of "future" is ordinary clock skew, not a wrong time zone. */
    const val FUTURE_SLACK_MILLIS = 20L * 60 * 1000

    const val BANGLADESH_OFFSET_MILLIS = 6L * 60 * 60 * 1000
    const val INDIA_OFFSET_MILLIS = 5L * 60 * 60 * 1000 + 30L * 60 * 1000
    const val MYANMAR_OFFSET_MILLIS = 6L * 60 * 60 * 1000 + 30L * 60 * 1000

    private const val MIN_FUTURE_ITEMS = 3
    private const val PREFS = "feed_clock"
    private const val KEY_LOCAL_AS_UTC = "local_as_utc"

    /** Feed URLs learned to write local time under a UTC label (kept across restarts once attached). */
    private val learnedLocalAsUtc: MutableSet<String> = ConcurrentHashMap.newKeySet()
    @Volatile private var prefs: SharedPreferences? = null

    /** Loads what was learned in earlier runs. Without it (unit tests) learning lasts for the process. */
    fun attach(context: Context) {
        if (prefs != null) return
        val store = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        learnedLocalAsUtc += store.getStringSet(KEY_LOCAL_AS_UTC, emptySet()).orEmpty()
        prefs = store
    }

    /** The usual UTC offset of feeds from [region]. */
    fun localOffsetOf(region: NewsRegion?): Long = when (region) {
        NewsRegion.INDIA -> INDIA_OFFSET_MILLIS
        NewsRegion.MYANMAR -> MYANMAR_OFFSET_MILLIS
        else -> BANGLADESH_OFFSET_MILLIS
    }

    /**
     * Bing: reads each stated wall-clock time as Pacific time. If that would put the newest item
     * in the future, Bing has fixed its feed, and the stated times are kept as they are.
     */
    fun fixBing(articles: List<NewsArticle>, now: Long = System.currentTimeMillis()): List<NewsArticle> {
        if (articles.isEmpty()) return articles
        val shifted = articles.map { it.copy(timestamp = it.timestamp - PACIFIC.getOffset(it.timestamp)) }
        return if (shifted.maxOf { it.timestamp } > now + FUTURE_SLACK_MILLIS) articles else shifted
    }

    /**
     * Feeds that write local time under a UTC label: moves their UTC-labelled items back by
     * [localOffsetMillis]. Applies to feeds marked [knownBroken] and to feeds caught in the act:
     * at least [MIN_FUTURE_ITEMS] items (and a fifth of the feed) "published" in the future by no
     * more than the local offset. Only items whose date really carries a UTC label are moved, so
     * the repair stops by itself once the site starts writing "+0600".
     */
    fun fixLocalLabelledAsUtc(
        feedUrl: String,
        knownBroken: Boolean,
        localOffsetMillis: Long,
        articles: List<NewsArticle>,
        now: Long = System.currentTimeMillis()
    ): List<NewsArticle> {
        if (articles.isEmpty()) return articles
        if (!knownBroken && feedUrl !in learnedLocalAsUtc) {
            val future = articles.count {
                hasUtcLabel(it.pubDate) &&
                    it.timestamp > now + FUTURE_SLACK_MILLIS &&
                    it.timestamp <= now + localOffsetMillis + FUTURE_SLACK_MILLIS
            }
            if (future < MIN_FUTURE_ITEMS || future * 5 < articles.size) return articles
            learn(feedUrl)
        }
        return articles.map { if (hasUtcLabel(it.pubDate)) it.copy(timestamp = it.timestamp - localOffsetMillis) else it }
    }

    /** True for "…GMT", "…UTC", "…+0000", "…+00:00", "…Z". */
    internal fun hasUtcLabel(rawDate: String): Boolean {
        val date = rawDate.trim()
        return date.endsWith("GMT", ignoreCase = true) || date.endsWith("UTC", ignoreCase = true) ||
            date.endsWith("+0000") || date.endsWith("+00:00") || date.endsWith("Z")
    }

    private fun learn(feedUrl: String) {
        if (!learnedLocalAsUtc.add(feedUrl)) return
        prefs?.edit()?.putStringSet(KEY_LOCAL_AS_UTC, learnedLocalAsUtc.toSet())?.apply()
    }

    /** For tests: forget everything learned in this process. */
    internal fun resetForTest() = learnedLocalAsUtc.clear()
}
