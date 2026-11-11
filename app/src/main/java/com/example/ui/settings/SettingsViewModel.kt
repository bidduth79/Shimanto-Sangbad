package com.example.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GoogleNewsDecoder
import com.example.BuildConfig
import com.example.data.NewsRepository
import com.example.data.NewsSources
import com.example.data.YouTubeRepository
import com.example.data.RssFeedItem
import com.example.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SettingsUiState(
    val keywords: List<String> = emptyList(),
    val rssFeeds: List<RssFeedItem> = emptyList(),
    val youtubeApiKey: String = "",
    val youtubeChannelIds: String = "",
    val isDarkMode: Boolean = true,
    /** YouTube API units spent today and Google's free daily allowance. */
    val quotaUsedToday: Int = 0,
    val quotaDailyLimit: Int = 10_000,
    /** Built-in news channels plus the user's own channel IDs. */
    val channelCount: Int = 0,
    val appVersion: String = "",
    val showNewBadges: Boolean = true,
    val lastSeenHomeAt: Long = 0L,
    val lastSeenVideoAt: Long = 0L,
    val alertsEnabled: Boolean = true,
    val translateHeadlines: Boolean = true,
    val alertKeywords: List<String> = emptyList(),
    /** What every news source last did (see SourceHealth). */
    val sourceStatuses: List<com.example.data.SourceHealth.Status> = emptyList(),
    val lastRefreshRoundAt: Long = 0L
) {
    val hasYoutubeApiKey: Boolean get() = youtubeApiKey.isNotBlank()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val newsRepository: NewsRepository,
    private val linkDecoder: GoogleNewsDecoder,
    private val youTubeRepository: YouTubeRepository,
    private val videoSync: com.example.data.VideoSync,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        settingsRepository.youtubeApiKey,
        youTubeRepository.quotaUsedToday,
        com.example.data.SourceHealth.statuses,
        com.example.data.SourceHealth.lastRoundAt
    ) { s, apiKey, quotaUsed, sources, lastRoundAt ->
        val customChannels = s.youtubeChannelIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        SettingsUiState(
            keywords = s.keywords,
            rssFeeds = s.rssFeeds,
            youtubeApiKey = apiKey,
            youtubeChannelIds = s.youtubeChannelIds,
            isDarkMode = s.isDarkMode,
            quotaUsedToday = quotaUsed,
            quotaDailyLimit = youTubeRepository.dailyQuotaLimit,
            channelCount = (NewsSources.youtubeNewsChannels + NewsSources.youtubeIndianChannels + customChannels).distinct().size,
            appVersion = BuildConfig.VERSION_NAME,
            showNewBadges = s.showNewBadges,
            lastSeenHomeAt = s.lastSeenHomeAt,
            lastSeenVideoAt = s.lastSeenVideoAt,
            alertsEnabled = s.alertsEnabled,
            translateHeadlines = s.translateHeadlines,
            alertKeywords = s.alertKeywords,
            sourceStatuses = sources.values.toList(),
            lastRefreshRoundAt = lastRoundAt
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    fun addKeyword(keyword: String) = launch { settingsRepository.addKeyword(keyword) }
    fun removeKeyword(keyword: String) = launch { settingsRepository.removeKeyword(keyword) }
    fun addRssFeed(title: String, url: String) = launch { settingsRepository.addRssFeed(title, url) }
    fun removeRssFeed(url: String) = launch { settingsRepository.removeRssFeed(url) }
    fun saveYoutubeApiKey(key: String) = launch { settingsRepository.setYoutubeApiKey(key) }
    fun saveYoutubeChannelIds(ids: String) = launch { settingsRepository.setYoutubeChannelIds(ids) }
    fun toggleDarkMode() = launch { settingsRepository.setDarkMode(!uiState.value.isDarkMode) }
    fun toggleNewBadges() = launch { settingsRepository.setShowNewBadges(!uiState.value.showNewBadges) }
    fun markHomeSeen() = launch { settingsRepository.markHomeSeen() }
    fun toggleTranslate() = launch { settingsRepository.setTranslateHeadlines(!uiState.value.translateHeadlines) }
    fun toggleAlerts() = launch { settingsRepository.setAlertsEnabled(!uiState.value.alertsEnabled) }
    fun addAlertKeyword(keyword: String) = launch { settingsRepository.addAlertKeyword(keyword) }
    fun removeAlertKeyword(keyword: String) = launch { settingsRepository.removeAlertKeyword(keyword) }

    /** True the first time only: the notification permission is asked for once per install. */
    suspend fun claimNotificationPermissionAsk(): Boolean {
        if (settingsRepository.notificationPermissionAsked()) return false
        settingsRepository.setNotificationPermissionAsked()
        return true
    }
    fun markVideoSeen() = launch { settingsRepository.markVideoSeen() }

    /** Clears cached news (bookmarks are kept), decoded links and the HTTP/image cache. */
    fun clearCache(onDone: () -> Unit) = launch {
        newsRepository.clearCache()
        videoSync.clear()
        linkDecoder.clearCache()
        withContext(Dispatchers.IO) { runCatching { context.cacheDir.deleteRecursively() } }
        onDone()
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
