package com.gabrieltagama.menuplanner.core.domain.port

import com.gabrieltagama.menuplanner.core.domain.model.RepeatWindow
import com.gabrieltagama.menuplanner.core.domain.model.ThemeMode
import com.gabrieltagama.menuplanner.core.domain.query.UserPreferencesQueries

/**
 * Persistence port for what the user chooses in Ajustes: the changes of the theme mode and of the
 * days without repeating a dish, on top of reading them (UserPreferencesQueries). Implemented in
 * :core:data.
 */
interface UserPreferencesRepository : UserPreferencesQueries {
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setRepeatWindow(window: RepeatWindow)
}
