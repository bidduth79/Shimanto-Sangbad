package com.example.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.NewsDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

private fun isBengali(text: String): Boolean {
    return text.any { it in 'ঀ'..'৿' }
}

class NewsWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val repository = NewsRepository()
        val keywordPrefs = KeywordPreferences(applicationContext)
        val database = NewsDatabase.getDatabase(applicationContext)

        return try {
            val maxAgeMillis = 36L * 60 * 60 * 1000L
            val now = System.currentTimeMillis()
            database.newsDao().deleteOldNews(now - maxAgeMillis)

            val keywords = keywordPrefs.getKeywords()
            val socialExclusions = "-site:facebook.com -site:fb.com"

            val bgbArticles = supervisorScope {
                keywords.map { keyword ->
                    async(Dispatchers.IO) {
                        if (isBengali(keyword)) {
                            repository.fetchNews("$keyword $socialExclusions", hl = "bn", gl = "BD", ceid = "BD:bn", whenWindow = "1d")
                                .map { it.copy(matchedKeyword = keyword) }
                        } else {
                            repository.fetchNews("$keyword $socialExclusions", hl = "en-BD", gl = "BD", ceid = "BD:en", whenWindow = "1d")
                                .map { it.copy(matchedKeyword = keyword) }
                        }
                    }
                }.awaitAll().flatten()
            }

            val fbArticles = supervisorScope {
                keywords.map { keyword ->
                    async(Dispatchers.IO) {
                        repository.fetchFacebookNews(keyword)
                    }
                }.awaitAll().flatten()
            }

            val savedLinks = database.newsDao().getSavedNews().map { it.link }.toSet()
            val updatedBgb = bgbArticles
                .filter { it.timestamp > 0L && (now - it.timestamp) in -600_000L..maxAgeMillis }
                .distinctBy { it.link }
                .sortedByDescending { it.timestamp }
                .map { it.copy(category = "bgb", isSaved = savedLinks.contains(it.link)) }

            val updatedFb = fbArticles
                .filter { it.timestamp > 0L && (now - it.timestamp) in -600_000L..maxAgeMillis }
                .distinctBy { it.link }
                .sortedByDescending { it.timestamp }
                .map { it.copy(category = "facebook", isSaved = savedLinks.contains(it.link)) }

            if (updatedBgb.isNotEmpty()) {
                database.newsDao().insertAll(updatedBgb)
            }
            if (updatedFb.isNotEmpty()) {
                database.newsDao().insertAll(updatedFb)
            }

            Log.d("NewsWorker", "Successfully fetched and cached ${updatedBgb.size} background news items")
            Result.success()
        } catch (e: Exception) {
            Log.e("NewsWorker", "Error updating news", e)
            Result.retry()
        }
    }
}