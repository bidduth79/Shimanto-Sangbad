import os

filepath = "app/src/main/java/com/example/data/NewsWorker.kt"

new_worker_code = """package com.example.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class NewsWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val repository = NewsRepository()
        val keywordPrefs = KeywordPreferences(applicationContext)
        val firebaseManager = FirebaseManager(applicationContext)
        
        return try {
            val keywords = keywordPrefs.getKeywords()
            val bgbArticles = mutableListOf<NewsArticle>()
            
            for (keyword in keywords) {
                bgbArticles.addAll(repository.fetchNews(keyword, hl = "bn", gl = "BD", ceid = "BD:bn"))
                bgbArticles.addAll(repository.fetchNews(keyword, hl = "en-BD", gl = "BD", ceid = "BD:en"))
                bgbArticles.addAll(repository.fetchNews(keyword, hl = "bn", gl = "IN", ceid = "IN:bn"))
                bgbArticles.addAll(repository.fetchNews(keyword, hl = "en-IN", gl = "IN", ceid = "IN:en"))
            }
            
            val savedLinks = firebaseManager.getSavedNewsLinks().toSet()
            val updatedBgb = bgbArticles.distinctBy { it.link }.sortedByDescending { it.timestamp }
                .map { it.copy(category = "bgb", isSaved = savedLinks.contains(it.link)) }
            
            // Note: If we had a local DB injection here, we could save it. But for now, returning success.
            Log.d("NewsWorker", "Successfully fetched background news")
            Result.success()
        } catch (e: Exception) {
            Log.e("NewsWorker", "Error updating news", e)
            Result.retry()
        }
    }
}"""

with open(filepath, "w") as f:
    f.write(new_worker_code)
