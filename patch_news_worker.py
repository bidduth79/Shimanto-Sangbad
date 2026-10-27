import os

filepath = "app/src/main/java/com/example/data/NewsWorker.kt"
with open(filepath, "r") as f:
    content = f.read()

new_worker_code = """package com.example.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class NewsWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val repository = NewsRepository()
        val firebaseManager = FirebaseManager(applicationContext)
        val keywordPrefs = KeywordPreferences(applicationContext)
        
        return try {
            val apiUrl = "https://script.google.com/macros/s/AKfycbwdlKVUQig7h3a9HftRCWeOc_3j11CTxGIwqSbrW1u2d0hb9zObRziDg-zb25fIw638/exec"
            val allArticles = repository.fetchFromApi(apiUrl)
            
            // Just fetching keeps the cache/API warm and verifies connectivity.
            // If we had push notifications, we would trigger them here if new articles exist.
            
            Log.d("NewsWorker", "Successfully fetched ${allArticles.size} articles in background.")
            Result.success()
        } catch (e: Exception) {
            Log.e("NewsWorker", "Error updating news", e)
            Result.retry()
        }
    }
}"""

with open(filepath, "w") as f:
    f.write(new_worker_code)
