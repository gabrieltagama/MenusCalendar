package com.gabrieltagama.menuplanner

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.gabrieltagama.menuplanner.core.domain.common.ApplicationScope
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.PrepareRecipeBookUseCase
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Application entry point that bootstraps the Hilt dependency graph and gives WorkManager the
 * Hilt worker factory, so background workers such as the daily recipe backup get their
 * dependencies injected. WorkManager is initialized on demand (its default initializer is
 * removed in the manifest). On start it prepares the recipe book (starter dishes on the first
 * launch, standard units) in the process-wide scope, so it is never cancelled with a screen.
 */
@HiltAndroidApp
class MenuPlannerApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var prepareRecipeBook: PrepareRecipeBookUseCase

    @Inject
    @field:ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { prepareRecipeBook() }
    }
}
