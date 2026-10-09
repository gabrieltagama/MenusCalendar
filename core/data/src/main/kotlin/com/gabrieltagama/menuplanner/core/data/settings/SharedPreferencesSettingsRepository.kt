package com.gabrieltagama.menuplanner.core.data.settings

import android.content.Context
import androidx.core.content.edit
import com.gabrieltagama.menuplanner.core.domain.model.ThemeMode
import com.gabrieltagama.menuplanner.core.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Settings stored in private SharedPreferences. The theme is read once when the repository is
 * created, so it is available synchronously at app start; unknown stored values fall back to SYSTEM.
 */
@Singleton
internal class SharedPreferencesSettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) : SettingsRepository {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val mode = MutableStateFlow(readThemeMode())

    override val themeMode: StateFlow<ThemeMode> = mode.asStateFlow()

    override suspend fun setThemeMode(mode: ThemeMode) {
        this.mode.value = mode
        withContext(Dispatchers.IO) { preferences.edit { putString(KEY_THEME_MODE, mode.name) } }
    }

    private fun readThemeMode(): ThemeMode =
        ThemeMode.entries.firstOrNull { it.name == preferences.getString(KEY_THEME_MODE, null) } ?: ThemeMode.SYSTEM

    private companion object {
        const val PREFERENCES_NAME = "menu_planner_settings"
        const val KEY_THEME_MODE = "theme_mode"
    }
}
