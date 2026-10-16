package com.gabrieltagama.menuplanner

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point that bootstraps the Hilt dependency graph, runs the start-up work
 * (AppStartup) and gives WorkManager the Hilt worker factory, so background workers such as the
 * daily backup get their dependencies injected. WorkManager is initialized on demand (its default
 * initializer is removed in the manifest).
 */
@HiltAndroidApp
class MenuPlannerApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var startup: AppStartup

    override fun onCreate() {
        super.onCreate()
        startup.start()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}
