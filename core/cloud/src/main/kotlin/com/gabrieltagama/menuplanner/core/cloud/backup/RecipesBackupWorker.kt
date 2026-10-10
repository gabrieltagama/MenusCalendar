package com.gabrieltagama.menuplanner.core.cloud.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.usecase.backup.BackUpRecipesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Daily background job of the recipe backup: it uploads only when the recipes changed since the
 * last upload. Network problems are retried by WorkManager; a missing authorization is not, the
 * user has to link the account again from Ajustes.
 */
@HiltWorker
internal class RecipesBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val backUpRecipes: BackUpRecipesUseCase
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result = when (val outcome = backUpRecipes(onlyIfChanged = true)) {
        is Outcome.Success -> Result.success()
        is Outcome.Failure -> if (outcome.error == DomainError.CloudUnavailable) Result.retry() else Result.failure()
    }
}
