package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GoogleNewsDecoder
import com.example.data.NewsArticle
import com.example.data.NewsFilters
import com.example.data.NewsRegionClassifier
import com.example.data.NewsRepository
import com.example.data.settings.SettingsRepository
import com.example.ui.groupNewsByRegion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Home, live-social, RSS and bookmark screens. Reads everything from Room/DataStore flows; the only
 * thing it does imperatively is ask the repository to refresh.
 */
@HiltViewModel
class NewsViewModel @Inject constructor(
    private val newsAlerts: com.example.data.NewsAlerts,
    @param:dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
    private val newsRepository: NewsRepository,
    private val settingsRepository: SettingsRepository,
    private val linkDecoder: GoogleNewsDecoder
) : ViewModel() {

    private val selection = MutableStateFlow(HomeSelection())
    private val isRefreshing = MutableStateFlow(false)
    private var enrichJob: kotlinx.coroutines.Job? = null
    private var refreshJob: Job? = null

    private val keywords = settingsRepository.settings.map { it.keywords }.distinctUntilChanged()

    /**
     * Only articles whose headline is about one of the user's current keywords. Applied on read as
     * well as on write, so rows stored by older, looser rules (or before a keyword was removed)
     * disappear immediately. Shared so the start-up wait below and the screen use one query.
     */
    private val regionalNews = combine(newsRepository.news, keywords) { news, kws ->
        groupNewsByRegion(NewsFilters.onlyAboutKeywords(news, kws))
    }
        .flowOn(Dispatchers.Default)
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private val inputs = combine(
        regionalNews,
        combine(newsRepository.facebookNews, keywords) { fb, kws -> NewsFilters.onlyAboutKeywords(fb, kws) },
        newsRepository.customFeedNews,
        newsRepository.savedNews,
        settingsRepository.settings
    ) { regional, facebook, customFeeds, saved, settings ->
        HomeInputs(
            regional = regional,
            facebookNews = facebook,
            customFeedNews = customFeeds,
            savedNews = saved,
            keywords = settings.keywords,
            rssFeeds = settings.rssFeeds,
            visitedLinks = settings.visitedLinks
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(inputs, selection, isRefreshing, ::buildHomeState)
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch {
            newsRepository.restoreBookmarksFromCloud()
            // Let cached news reach the screen before the network work starts competing for CPU
            regionalNews.first()
            newsRepository.pruneOffTopic(settingsRepository.current().keywords)
            newsRepository.backfillRegions(settingsRepository.regionRulesVersion())
            settingsRepository.setRegionRulesVersion(NewsRegionClassifier.VERSION)
            val sinceLastSync = System.currentTimeMillis() - settingsRepository.lastSyncAt()
            if (sinceLastSync > STARTUP_REFRESH_AFTER_MILLIS) refresh() else fillImages()
        }
        // Re-fetch when the user changes what to look for
        viewModelScope.launch {
            settingsRepository.settings.map { it.keywords to it.rssFeeds }
                .distinctUntilChanged()
                .drop(1)
                .collect { refresh() }
        }
        // Quiet periodic refresh while the app is open
        viewModelScope.launch {
            while (true) {
                delay(AUTO_REFRESH_MILLIS)
                refresh()
            }
        }
    }

    /** Starts a full refresh unless one is already running. */
    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            isRefreshing.value = true
            try {
                val settings = settingsRepository.current()
                newsRepository.syncAll(settings.keywords, settings.rssFeeds) {
                    // Bangladesh, social and own feeds are in: stop the spinner, the rest fills in quietly
                    isRefreshing.value = false
                }
                settingsRepository.setLastSyncAt(System.currentTimeMillis())
            } finally {
                isRefreshing.value = false
            }
            runCatching { newsAlerts.checkAndNotify() }
            com.example.widget.BorderNewsWidget.refreshAll(appContext)
            // Link decoding and pictures can take minutes (Google rate limits). They run on their own
            // so they never hold back the next refresh: a refresh is only "running" while fetching.
            enrichJob?.cancel()
            enrichJob = viewModelScope.launch {
                runCatching { preResolveTopLinks() }
                runCatching { newsRepository.fillMissingImages() }
            }
        }
    }

    /** Back in the app after a while (the timer below pauses while the phone sleeps): fetch now. */
    fun refreshIfStale() {
        viewModelScope.launch {
            if (System.currentTimeMillis() - settingsRepository.lastSyncAt() > STARTUP_REFRESH_AFTER_MILLIS) refresh()
        }
    }

    /** Pictures for cached articles that don't have one yet (a refresh does this on its own). */
    private fun fillImages() {
        viewModelScope.launch { runCatching { newsRepository.fillMissingImages() } }
    }

    fun selectTab(tab: HomeTab) = selection.update { it.copy(tab = tab, filter = HomeFilter.ALL) }

    fun selectFilter(filter: String) = selection.update { it.copy(filter = filter) }

    fun updateSearchQuery(query: String) = selection.update { it.copy(searchQuery = query) }

    /** Shows only stories about [keyword]; null shows all news again. */
    fun selectKeyword(keyword: String?) = selection.update { it.copy(keyword = keyword) }

    /** Limits news and videos to the last [hours] hours; null shows everything again. */
    fun selectMaxAge(hours: Int?) = selection.update { it.copy(maxAgeHours = hours) }

    /** Country tabs: show only stories not yet read. */
    fun toggleOnlyUnread() = selection.update { it.copy(onlyUnread = !it.onlyUnread) }

    fun toggleFilterChips() = selection.update { it.copy(areFilterChipsVisible = !it.areFilterChipsVisible) }

    fun toggleBookmark(article: NewsArticle) {
        viewModelScope.launch { newsRepository.toggleBookmark(article) }
    }

    fun markVisited(article: NewsArticle) {
        viewModelScope.launch { settingsRepository.markLinkVisited(article.link) }
    }

    /** Resolves the publisher URL behind a Google News link (cached) and hands it to [onResolved]. */
    fun resolveLink(article: NewsArticle, onResolved: (String) -> Unit) {
        viewModelScope.launch {
            onResolved(linkDecoder.resolve(article.originalLink ?: article.link))
        }
    }

    /** Decodes the first few links in the background so copy/share on them is instant. */
    private suspend fun preResolveTopLinks() {
        uiState.value.visibleArticles.take(PRE_RESOLVE_COUNT).forEach {
            runCatching { linkDecoder.resolve(it.originalLink ?: it.link) }
        }
    }

    private companion object {
        const val AUTO_REFRESH_MILLIS = 5L * 60 * 1000
        const val STARTUP_REFRESH_AFTER_MILLIS = 5L * 60 * 1000
        const val PRE_RESOLVE_COUNT = 10
    }
}
