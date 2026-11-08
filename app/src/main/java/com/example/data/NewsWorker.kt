package com.example.data

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.settings.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodic background refresh so the app opens with recent news already cached.
 * Class name and package are kept stable: WorkManager stores them for already-scheduled work.
 */
@HiltWorker
class NewsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val newsRepository: NewsRepository,
    private val settingsRepository: SettingsRepository,
    private val videoSync: VideoSync
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = try {
        // The open app refreshes on its own; don't compete with it for network and CPU
        if (System.currentTimeMillis() - settingsRepository.lastSyncAt() < SKIP_IF_SYNCED_WITHIN_MILLIS) {
            Log.d(TAG, "Skipped: app refreshed recently")
        } else {
            newsRepository.syncForBackground(settingsRepository.current().keywords)
            Log.d(TAG, "Background news sync finished")
        }
        // Videos too, so the video tab opens with fresh ones (skipped if the app just did it)
        Log.d(TAG, "Background video sync: ${videoSync.sync(minIntervalMillis = SKIP_IF_SYNCED_WITHIN_MILLIS)}")
        Result.success()
    } catch (e: Exception) {
        Log.e(TAG, "Background news sync failed", e)
        Result.retry()
    }

    private companion object {
        const val TAG = "NewsWorker"
        const val SKIP_IF_SYNCED_WITHIN_MILLIS = 15L * 60 * 1000
    }
}
