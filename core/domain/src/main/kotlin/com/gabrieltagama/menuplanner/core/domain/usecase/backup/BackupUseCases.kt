package com.gabrieltagama.menuplanner.core.domain.usecase.backup

import com.gabrieltagama.menuplanner.core.domain.common.CloudError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.common.flatMap
import com.gabrieltagama.menuplanner.core.domain.common.map
import com.gabrieltagama.menuplanner.core.domain.common.onSuccess
import com.gabrieltagama.menuplanner.core.domain.model.BackupResult
import com.gabrieltagama.menuplanner.core.domain.model.CloudAccount
import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import com.gabrieltagama.menuplanner.core.domain.model.share.SnapshotFingerprint
import com.gabrieltagama.menuplanner.core.domain.model.share.SnapshotScope
import com.gabrieltagama.menuplanner.core.domain.port.BackupScheduler
import com.gabrieltagama.menuplanner.core.domain.port.CloudBackupRepository
import com.gabrieltagama.menuplanner.core.domain.port.MenuSnapshotCodec
import com.gabrieltagama.menuplanner.core.domain.port.MenuSnapshotRepository
import com.gabrieltagama.menuplanner.core.domain.usecase.share.ImportMenusUseCase
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Use cases of the optional backup of the recipe book; the calendar is never backed up (BACKUP_SCOPE).
 * Restoring imports the recipes of the backup like a shared file (newest updatedAt wins, standard
 * units) and ignores any calendar an older backup may contain; an automatic backup uploads only when
 * the snapshot fingerprint changed since the last upload.
 * Connecting restores, schedules the daily backup and uploads; the daily backup is scheduled only
 * once the restore succeeded, so it can never replace the backup with a recipe book that is still
 * missing the restored recipes. Without an access token (the user cancelled Google's consent)
 * connecting fails with AuthorizationCancelled. Disconnecting cancels the daily backup.
 */

class BackUpUseCase @Inject constructor(
    private val cloud: CloudBackupRepository,
    private val snapshots: MenuSnapshotRepository,
    private val codec: MenuSnapshotCodec,
    private val clock: Clock
) {
    suspend operator fun invoke(onlyIfChanged: Boolean): Outcome<BackupResult> {
        if (cloud.account.first() is CloudAccount.Disconnected) return Outcome.Success(BackupResult.NOT_CONNECTED)
        val snapshot = snapshots.read(BACKUP_SCOPE)
        val fingerprint = SnapshotFingerprint.of(snapshot)
        if (onlyIfChanged && fingerprint == cloud.lastUploadedFingerprint()) return Outcome.Success(BackupResult.NO_CHANGES)
        return cloud.upload(codec.encode(snapshot, clock.instant()), fingerprint).map { BackupResult.UPLOADED }
    }
}

class RestoreBackupUseCase @Inject constructor(
    private val cloud: CloudBackupRepository,
    private val importMenus: ImportMenusUseCase
) {
    suspend operator fun invoke(): Outcome<ImportSummary> =
        cloud.download().flatMap { text -> text?.let { importMenus(it, BACKUP_SCOPE) } ?: Outcome.Success(ImportSummary.EMPTY) }
}

class ConnectCloudAccountUseCase @Inject constructor(
    private val cloud: CloudBackupRepository,
    private val restoreBackup: RestoreBackupUseCase,
    private val backUp: BackUpUseCase,
    private val scheduler: BackupScheduler
) {
    suspend operator fun invoke(accessToken: String?): Outcome<ImportSummary> {
        if (accessToken == null) return Outcome.Failure(CloudError.AuthorizationCancelled)
        return cloud.connect(accessToken)
            .flatMap { restoreBackup() }
            .onSuccess { scheduler.scheduleDaily() }
            .flatMap { summary -> backUp(onlyIfChanged = true).map { summary } }
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

private val BACKUP_SCOPE = SnapshotScope.RECIPES
