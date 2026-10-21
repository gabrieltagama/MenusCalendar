package com.gabrieltagama.menuplanner.core.cloud.di

import com.gabrieltagama.menuplanner.core.cloud.auth.DriveTokenProvider
import com.gabrieltagama.menuplanner.core.cloud.auth.GoogleDriveAuthorizer
import com.gabrieltagama.menuplanner.core.cloud.backup.GoogleDriveBackupRepository
import com.gabrieltagama.menuplanner.core.cloud.backup.WorkManagerBackupScheduler
import com.gabrieltagama.menuplanner.core.cloud.drive.DriveAppDataClient
import com.gabrieltagama.menuplanner.core.cloud.drive.DriveFiles
import com.gabrieltagama.menuplanner.core.domain.port.BackupScheduler
import com.gabrieltagama.menuplanner.core.domain.port.CloudBackupRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the cloud backup ports to the Google Drive and WorkManager implementations, and the Drive
 * token and file operations the backup uses to their Google implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class CloudModule {

    @Binds
    @Singleton
    abstract fun bindCloudBackupRepository(implementation: GoogleDriveBackupRepository): CloudBackupRepository

    @Binds
    abstract fun bindBackupScheduler(implementation: WorkManagerBackupScheduler): BackupScheduler

    @Binds
    abstract fun bindDriveTokenProvider(implementation: GoogleDriveAuthorizer): DriveTokenProvider

    @Binds
    abstract fun bindDriveFiles(implementation: DriveAppDataClient): DriveFiles
}
