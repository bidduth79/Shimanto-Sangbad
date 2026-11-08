package com.example

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Hilt entry point. Also supplies WorkManager's configuration so workers get their dependencies
 * injected (the default WorkManager initializer is removed in the manifest).
 */
@HiltAndroidApp
class SimantoApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        scheduleBackgroundNewsSync(this)
    }
}
