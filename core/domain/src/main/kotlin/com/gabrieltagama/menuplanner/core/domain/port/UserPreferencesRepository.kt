package com.gabrieltagama.menuplanner.core.domain.port

import com.gabrieltagama.menuplanner.core.domain.model.RepeatWindow
import com.gabrieltagama.menuplanner.core.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Persistence port for what the user chooses in Ajustes, read without blocking: the theme mode and
 * the days without repeating a dish of the autofill (RepeatWindow). Implemented in :core:data.
 */
interface UserPreferencesRepository {
    val themeMode: Flow<ThemeMode>
    val repeatWindow: Flow<RepeatWindow>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setRepeatWindow(window: RepeatWindow)
}
