package com.gabrieltagama.menuplanner.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.gabrieltagama.menuplanner.core.domain.common.IoDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Provides the single Preferences DataStore of the app settings. On first use it takes over the
 * values of the two SharedPreferences files used before (settings and the initial recipe book
 * flag), so nobody loses the chosen theme or sees the welcome screen again.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class SettingsDataStore

@Module
@InstallIn(SingletonComponent::class)
internal object DataStoreModule {

    private const val SETTINGS_FILE = "menu_planner_settings"
    private const val LEGACY_INITIAL_DATA_FILE = "menu_planner_initial_data"

    @Provides
    @Singleton
    @SettingsDataStore
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): DataStore<Preferences> = settingsDataStore(context, CoroutineScope(ioDispatcher + SupervisorJob()))

    fun settingsDataStore(
        context: Context,
        scope: CoroutineScope
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        migrations = listOf(
            SharedPreferencesMigration(context, SETTINGS_FILE),
            SharedPreferencesMigration(context, LEGACY_INITIAL_DATA_FILE)
        ),
        scope = scope,
        produceFile = { context.preferencesDataStoreFile(SETTINGS_FILE) }
    )
}
