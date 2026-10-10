package com.gabrieltagama.menuplanner.core.cloud.di

import com.gabrieltagama.menuplanner.core.cloud.backup.GoogleDriveBackupRepository
import com.gabrieltagama.menuplanner.core.cloud.backup.WorkManagerBackupScheduler
import com.gabrieltagama.menuplanner.core.domain.repository.BackupScheduler
import com.gabrieltagama.menuplanner.core.domain.repository.CloudBackupRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the cloud backup ports to the Google Drive and WorkManager implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class CloudModule {

    @Binds
    @Singleton
    abstract fun bindCloudBackupRepository(implementation: GoogleDriveBackupRepository): CloudBackupRepository

    @Binds
    abstract fun bindBackupScheduler(implementation: WorkManagerBackupScheduler): BackupScheduler
}
