package com.gabrieltagama.menuplanner.core.cloud.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.gabrieltagama.menuplanner.core.domain.repository.BackupScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Runs RecipesBackupWorker once a day with WorkManager, only when there is network. KEEP leaves
 * an already scheduled job untouched, so scheduling again is harmless.
 */
internal class WorkManagerBackupScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) : BackupScheduler {

    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    override fun scheduleDaily() {
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, dailyRequest())
    }

    override fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    private fun dailyRequest(): PeriodicWorkRequest =
        PeriodicWorkRequestBuilder<RecipesBackupWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

    private companion object {
        const val WORK_NAME = "recipes-daily-backup"
    }
}
