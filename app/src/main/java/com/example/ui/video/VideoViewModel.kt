package com.example.ui.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.KeywordMatcher
import com.example.data.NewsSources
import com.example.data.VideoSync
import com.example.data.VideoSyncOutcome
import com.example.data.YouTubeSearchResult
import com.example.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Why no videos could be shown; the screen turns this into text. */
sealed interface VideoError {
    data object NoResults : VideoError
    data class LoadFailed(val detail: String?) : VideoError
}

data class VideoUiState(
    /** Nothing cached yet and the first refresh is running: show skeleton cards. */
    val isLoading: Boolean = true,
    /** A refresh is running over an already-shown list: show the thin progress bar. */
    val isRefreshing: Boolean = false,
    val videos: List<YouTubeSearchResult> = emptyList(),
    val error: VideoError? = null
)

/**
 * The video tab reads only the cached list in Room, so it opens instantly; [VideoSync] refreshes
 * that cache in the background (shortly after launch, when the tab opens, every few minutes while
 * the app is used, and from the background worker) and new videos appear as each source answers.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class VideoViewModel @Inject constructor(
    private val videoSync: VideoSync,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val cachedVideos = combine(
        videoSync.observeVideos(),
        settingsRepository.settings.map { it.keywords }.distinctUntilChanged()
    ) { rows, keywords ->
        // Only what the current keywords are about (the cache may hold videos of removed keywords)
        val keepFrom = System.currentTimeMillis() - VideoSync.KEEP_MILLIS
        rows.asSequence()
            .filter { it.publishedMillis >= keepFrom }
            .map { it.toResult() }
            .filter { keywords.isEmpty() || KeywordMatcher.matches(it.snippet?.title.orEmpty(), keywords) }
            .toList()
    }
        // during a refresh rows arrive in many small batches; redraw a few times a second, not per batch
        .debounce(300)
        .flowOn(Dispatchers.Default)

    val uiState: StateFlow<VideoUiState> = combine(
        cachedVideos,
        videoSync.isSyncing,
        videoSync.lastOutcome
    ) { videos, syncing, outcome ->
        VideoUiState(
            isLoading = videos.isEmpty() && (syncing || outcome == null),
            isRefreshing = syncing && videos.isNotEmpty(),
            videos = videos,
            error = if (videos.isNotEmpty() || syncing) null else when (outcome) {
                VideoSyncOutcome.NoResults -> VideoError.NoResults
                is VideoSyncOutcome.Failed -> VideoError.LoadFailed(outcome.detail)
                else -> null
            }
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, VideoUiState())

    /** Links of videos already played (shared with the news "visited" list). */
    val watchedLinks: StateFlow<Set<String>> = settingsRepository.settings
        .map { it.visitedLinks }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Built-in newspaper/TV channels plus the user's own channel IDs. */
    val mediaChannelIds: StateFlow<Set<String>> = settingsRepository.settings
        .map { s ->
            (NewsSources.youtubeNewsChannels + s.youtubeChannelIds.split(",").map { it.trim() })
                .filter { it.isNotEmpty() }
                .toSet()
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NewsSources.youtubeNewsChannels.toSet())

    private var tabOpened = false

    init {
        // Prefetch: once the news has had its head start, bring videos too so the tab is ready
        viewModelScope.launch {
            delay(PREFETCH_DELAY_MILLIS)
            videoSync.sync()
        }
        // New keywords, channels or API key: fetch again (the first value is the current state)
        viewModelScope.launch {
            combine(settingsRepository.settings, settingsRepository.youtubeApiKey) { s, key ->
                Triple(s.keywords, s.youtubeChannelIds, key)
            }.distinctUntilChanged().drop(1).collect { videoSync.sync(force = true) }
        }
    }

    /** The video tab became visible: refresh if the cache is stale, then keep it fresh. */
    fun onScreenVisible() {
        viewModelScope.launch { videoSync.sync() }
        if (tabOpened) return
        tabOpened = true
        viewModelScope.launch {
            while (true) {
                delay(AUTO_REFRESH_MILLIS)
                videoSync.sync()
            }
        }
    }

    /** Back in the app: refresh videos when the cache is stale (no-op when fresh). */
    fun refreshIfStale() {
        viewModelScope.launch { videoSync.sync() }
    }

    /** Pull-to-refresh / retry / header refresh. */
    fun refresh() {
        viewModelScope.launch { videoSync.sync(force = true) }
    }

    fun markWatched(videoId: String) {
        viewModelScope.launch { settingsRepository.markLinkVisited(watchedLinkOf(videoId)) }
    }

    private companion object {
        // after the news has had its first refresh to itself
        const val PREFETCH_DELAY_MILLIS = 20_000L
        const val AUTO_REFRESH_MILLIS = 10L * 60 * 1000
    }
}
