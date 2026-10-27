package com.example

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.NewsWorker
import java.util.concurrent.TimeUnit

fun scheduleBackgroundNewsSync(context: Context) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val newsWorkRequest = PeriodicWorkRequestBuilder<NewsWorker>(15, TimeUnit.MINUTES)
        .setConstraints(constraints)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "NewsSyncWork",
        ExistingPeriodicWorkPolicy.KEEP,
        newsWorkRequest
    )
}
