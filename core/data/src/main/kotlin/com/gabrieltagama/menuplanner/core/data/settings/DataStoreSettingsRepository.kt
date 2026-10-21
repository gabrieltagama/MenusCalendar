package com.gabrieltagama.menuplanner.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gabrieltagama.menuplanner.core.data.di.SettingsDataStore
import com.gabrieltagama.menuplanner.core.domain.model.RepeatWindow
import com.gabrieltagama.menuplanner.core.domain.model.ThemeMode
import com.gabrieltagama.menuplanner.core.domain.port.SettingsRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Settings stored in a Preferences DataStore, read and written off the main thread. A file that
 * cannot be read behaves as empty (default settings) and unknown stored theme values fall back
 * to SYSTEM.
 */
@Singleton
internal class DataStoreSettingsRepository @Inject constructor(
    @param:SettingsDataStore private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private val preferences: Flow<Preferences> = dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }

    override val themeMode: Flow<ThemeMode> =
        preferences.map { stored -> ThemeMode.entries.firstOrNull { it.name == stored[THEME_MODE] } ?: ThemeMode.SYSTEM }.distinctUntilChanged()

    override val isOnboardingCompleted: Flow<Boolean> = preferences.map { it[ONBOARDING_COMPLETED] ?: false }.distinctUntilChanged()

    override val repeatWindow: Flow<RepeatWindow> =
        preferences.map { stored -> stored[REPEAT_WINDOW_DAYS]?.let { RepeatWindow.of(it) } ?: RepeatWindow.DEFAULT }.distinctUntilChanged()

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    override suspend fun setRepeatWindow(window: RepeatWindow) {
        dataStore.edit { it[REPEAT_WINDOW_DAYS] = window.days }
    }

    override suspend fun completeOnboarding() {
        dataStore.edit { it[ONBOARDING_COMPLETED] = true }
    }

    override suspend fun recipeUnitsVersion(): Int = preferences.first()[RECIPE_UNITS_VERSION] ?: 0

    override suspend fun setRecipeUnitsVersion(version: Int) {
        dataStore.edit { it[RECIPE_UNITS_VERSION] = version }
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val RECIPE_UNITS_VERSION = intPreferencesKey("recipe_units_version")
        val REPEAT_WINDOW_DAYS = intPreferencesKey("repeat_window_days")
    }
}
