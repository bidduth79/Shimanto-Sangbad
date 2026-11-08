package com.example.data

import android.util.Log
import com.example.data.local.VideoDao
import com.example.data.local.VideoEntity
import com.example.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

/** How the last video refresh ended. */
sealed interface VideoSyncOutcome {
    data object Skipped : VideoSyncOutcome
    data object Success : VideoSyncOutcome
    data object NoResults : VideoSyncOutcome
    data class Failed(val detail: String?) : VideoSyncOutcome
}

/**
 * Keeps the cached video list (Room) fresh. The video tab only ever reads the database, so it
 * opens instantly; refreshes write into it as each source answers. Used by the app (shortly after
 * launch and when the tab opens) and by the background worker; only one refresh runs at a time.
 */
@Singleton
class VideoSync @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
    private val videoDao: VideoDao,
    private val settingsRepository: SettingsRepository
) {
    private val running = Mutex()
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastOutcome = MutableStateFlow<VideoSyncOutcome?>(null)
    val lastOutcome: StateFlow<VideoSyncOutcome?> = _lastOutcome.asStateFlow()

    /** Cached videos from the last [KEEP_MILLIS], newest first. */
    fun observeVideos(now: Long = System.currentTimeMillis()) =
        videoDao.observeSince(now - KEEP_MILLIS)

    /**
     * Refreshes unless another refresh is running or (when not [force]d) the cache is younger than
     * [minIntervalMillis].
     */
    suspend fun sync(force: Boolean = false, minIntervalMillis: Long = MIN_INTERVAL_MILLIS): VideoSyncOutcome {
        if (!running.tryLock()) return VideoSyncOutcome.Skipped
        try {
            val now = System.currentTimeMillis()
            if (!force && now - videoDao.lastFetchedAt() < minIntervalMillis) return VideoSyncOutcome.Skipped
            _isSyncing.value = true

            val settings = settingsRepository.current()
            val apiKey = settingsRepository.youtubeApiKey.first()
            val result = youTubeRepository.fetchBgbVideos(
                keywords = settings.keywords,
                customApiKey = apiKey,
                customChannelIds = settings.youtubeChannelIds,
                onPartial = { batch -> store(batch) }
            )
            val outcome = when (result) {
                is YouTubeFetchResult.Success -> {
                    store(result.videos) // adds the channel countries looked up at the end
                    VideoSyncOutcome.Success
                }
                is YouTubeFetchResult.Error -> VideoSyncOutcome.NoResults
            }
            videoDao.deleteOlderThan(System.currentTimeMillis() - KEEP_MILLIS)
            return outcome.also { _lastOutcome.value = it }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Video refresh failed", e)
            return VideoSyncOutcome.Failed(e.localizedMessage).also { _lastOutcome.value = it }
        } finally {
            _isSyncing.value = false
            running.unlock()
        }
    }

    suspend fun clear() = videoDao.clear()

    private suspend fun store(videos: List<YouTubeSearchResult>) {
        val now = System.currentTimeMillis()
        videoDao.mergeAll(
            videos.mapNotNull { v ->
                VideoEntity.from(v, youTubeRepository.parseIsoTimestamp(v.snippet?.publishedAt), now)
            }
        )
    }

    companion object {
        private const val TAG = "VideoSync"
        /** Videos are kept (and searched) for 7 days. */
        const val KEEP_MILLIS = 7L * 24 * 60 * 60 * 1000
        /** A cache younger than this is fresh enough to skip a refresh. */
        const val MIN_INTERVAL_MILLIS = 5L * 60 * 1000
    }
}
