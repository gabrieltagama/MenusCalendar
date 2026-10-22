package com.gabrieltagama.menuplanner.core.domain.query

import com.gabrieltagama.menuplanner.core.domain.model.RepeatWindow
import com.gabrieltagama.menuplanner.core.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Read port of what the user chooses in Ajustes: the theme mode and the days without repeating a
 * dish of the autofill (RepeatWindow). Screens observe it directly; UserPreferencesRepository adds
 * the changes. Implemented in :core:data.
 */
interface UserPreferencesQueries {
    val themeMode: Flow<ThemeMode>
    val repeatWindow: Flow<RepeatWindow>
}
