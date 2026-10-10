package com.gabrieltagama.menuplanner.core.domain.usecase.backup

import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.BackupResult
import com.gabrieltagama.menuplanner.core.domain.model.CloudAccount
import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import com.gabrieltagama.menuplanner.core.domain.model.RecipesFingerprint
import com.gabrieltagama.menuplanner.core.domain.repository.BackupScheduler
import com.gabrieltagama.menuplanner.core.domain.repository.CloudBackupRepository
import com.gabrieltagama.menuplanner.core.domain.repository.DishRepository
import com.gabrieltagama.menuplanner.core.domain.repository.MenuShareRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/**
 * Use cases of the optional recipe backup. Only recipes are saved, never the calendar. Restoring
 * merges with the share rules (newest updatedAt wins); an automatic backup uploads only when the
 * recipes fingerprint changed since the last upload. Connecting restores, uploads and schedules
 * the daily backup; disconnecting cancels it.
 */
class ObserveCloudAccountUseCase @Inject constructor(private val cloud: CloudBackupRepository) {
    operator fun invoke(): StateFlow<CloudAccount> = cloud.account
}

class BackUpRecipesUseCase @Inject constructor(
    private val cloud: CloudBackupRepository,
    private val share: MenuShareRepository,
    private val dishes: DishRepository
) {
    suspend operator fun invoke(onlyIfChanged: Boolean): Outcome<BackupResult> {
        if (cloud.account.value is CloudAccount.Disconnected) return Outcome.Success(BackupResult.NOT_CONNECTED)
        val fingerprint = RecipesFingerprint.of(dishes.observeDishes().first())
        if (onlyIfChanged && fingerprint == cloud.lastUploadedFingerprint()) return Outcome.Success(BackupResult.NO_CHANGES)
        return when (val upload = cloud.upload(share.exportRecipes(), fingerprint)) {
            is Outcome.Failure -> upload
            is Outcome.Success -> Outcome.Success(BackupResult.UPLOADED)
        }
    }
}

class RestoreRecipesUseCase @Inject constructor(
    private val cloud: CloudBackupRepository,
    private val share: MenuShareRepository
) {
    suspend operator fun invoke(): Outcome<ImportSummary> = when (val download = cloud.download()) {
        is Outcome.Failure -> download
        is Outcome.Success -> download.value?.let { share.importAndMerge(it) } ?: Outcome.Success(ImportSummary.EMPTY)
    }
}

class ConnectCloudAccountUseCase @Inject constructor(
    private val cloud: CloudBackupRepository,
    private val restoreRecipes: RestoreRecipesUseCase,
    private val backUpRecipes: BackUpRecipesUseCase,
    private val scheduler: BackupScheduler
) {
    suspend operator fun invoke(accessToken: String): Outcome<ImportSummary> {
        val connection = cloud.connect(accessToken)
        if (connection is Outcome.Failure) return connection
        scheduler.scheduleDaily()
        val restored = restoreRecipes()
        if (restored is Outcome.Failure) return restored
        val backup = backUpRecipes(onlyIfChanged = true)
        return if (backup is Outcome.Failure) backup else restored
    }
}

class DisconnectCloudAccountUseCase @Inject constructor(
    private val cloud: CloudBackupRepository,
    private val scheduler: BackupScheduler
) {
    suspend operator fun invoke() {
        scheduler.cancel()
        cloud.disconnect()
    }
}
