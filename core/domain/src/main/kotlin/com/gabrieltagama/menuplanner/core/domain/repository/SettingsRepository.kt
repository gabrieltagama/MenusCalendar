package com.gabrieltagama.menuplanner.core.domain.repository

import com.gabrieltagama.menuplanner.core.domain.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow

/**
 * Persistence port for the user settings. Values are exposed as StateFlow so the theme is known
 * synchronously when the app starts, without a flash of the wrong colors. Implemented in :core:data.
 */
interface SettingsRepository {
    val themeMode: StateFlow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
