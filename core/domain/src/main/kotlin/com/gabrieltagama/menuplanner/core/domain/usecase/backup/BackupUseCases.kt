package com.gabrieltagama.menuplanner.core.domain.usecase.backup

import com.gabrieltagama.menuplanner.core.domain.common.Outcome
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
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Use cases of the optional backup of the recipe book and the calendar. Restoring imports the
 * backup like a shared file (newest updatedAt wins, standard units); an automatic backup uploads
 * only when the snapshot fingerprint changed since the last upload.
 * Connecting restores, uploads and schedules the daily backup; disconnecting cancels it.
 */
class ObserveCloudAccountUseCase @Inject constructor(private val cloud: CloudBackupRepository) {
    operator fun invoke(): Flow<CloudAccount> = cloud.account
}

class BackUpUseCase @Inject constructor(
    private val cloud: CloudBackupRepository,
    private val snapshots: MenuSnapshotRepository,
    private val codec: MenuSnapshotCodec,
    private val clock: Clock
) {
    suspend operator fun invoke(onlyIfChanged: Boolean): Outcome<BackupResult> {
        if (cloud.account.first() is CloudAccount.Disconnected) return Outcome.Success(BackupResult.NOT_CONNECTED)
        val snapshot = snapshots.read(SnapshotScope.EVERYTHING)
        val fingerprint = SnapshotFingerprint.of(snapshot)
        if (onlyIfChanged && fingerprint == cloud.lastUploadedFingerprint()) return Outcome.Success(BackupResult.NO_CHANGES)
        return when (val upload = cloud.upload(codec.encode(snapshot, Instant.now(clock)), fingerprint)) {
            is Outcome.Failure -> upload
            is Outcome.Success -> Outcome.Success(BackupResult.UPLOADED)
        }
    }
}

class RestoreBackupUseCase @Inject constructor(
    private val cloud: CloudBackupRepository,
    private val importMenus: ImportMenusUseCase
) {
    suspend operator fun invoke(): Outcome<ImportSummary> = when (val download = cloud.download()) {
        is Outcome.Failure -> download
        is Outcome.Success -> download.value?.let { importMenus(it) } ?: Outcome.Success(ImportSummary.EMPTY)
    }
}

class ConnectCloudAccountUseCase @Inject constructor(
    private val cloud: CloudBackupRepository,
    private val restoreBackup: RestoreBackupUseCase,
    private val backUp: BackUpUseCase,
    private val scheduler: BackupScheduler
) {
    suspend operator fun invoke(accessToken: String): Outcome<ImportSummary> {
        val connection = cloud.connect(accessToken)
        if (connection is Outcome.Failure) return connection
        scheduler.scheduleDaily()
        val restored = restoreBackup()
        if (restored is Outcome.Failure) return restored
        val backup = backUp(onlyIfChanged = true)
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
