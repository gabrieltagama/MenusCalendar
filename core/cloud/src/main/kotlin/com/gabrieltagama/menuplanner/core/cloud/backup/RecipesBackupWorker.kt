package com.gabrieltagama.menuplanner.core.cloud.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gabrieltagama.menuplanner.core.domain.common.CloudError
import com.gabrieltagama.menuplanner.core.domain.common.fold
import com.gabrieltagama.menuplanner.core.domain.usecase.backup.BackUpUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Daily background job of the recipe backup (the calendar is not backed up): it uploads only when something changed
 * since the last upload. Network problems are retried by WorkManager; a missing authorization is
 * not, the user has to link the account again from Ajustes. The class keeps its original name
 * because WorkManager stores it in the already scheduled work.
 */
@HiltWorker
internal class RecipesBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val backUp: BackUpUseCase
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result =
        backUp(onlyIfChanged = true).fold(
            onSuccess = { Result.success() },
            onFailure = { error -> if (error == CloudError.CloudUnavailable) Result.retry() else Result.failure() }
        )
}
