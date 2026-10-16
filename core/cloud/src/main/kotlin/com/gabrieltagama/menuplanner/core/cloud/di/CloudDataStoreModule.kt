package com.gabrieltagama.menuplanner.core.cloud.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Provides the Preferences DataStore of the cloud backup (linked email and fingerprint of the last
 * upload). It takes over the values of the SharedPreferences file used before.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class CloudDataStore

@Module
@InstallIn(SingletonComponent::class)
internal object CloudDataStoreModule {

    private const val FILE = "menu_planner_cloud_backup"

    @Provides
    @Singleton
    @CloudDataStore
    fun provideCloudDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            migrations = listOf(SharedPreferencesMigration(context, FILE)),
            produceFile = { context.preferencesDataStoreFile(FILE) }
        )
}
