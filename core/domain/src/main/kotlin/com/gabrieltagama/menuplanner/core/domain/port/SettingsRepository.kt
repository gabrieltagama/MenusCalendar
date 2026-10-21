package com.gabrieltagama.menuplanner.core.domain.port

import com.gabrieltagama.menuplanner.core.domain.model.RepeatWindow
import com.gabrieltagama.menuplanner.core.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Persistence port for the user settings, read without blocking: the theme mode, the days without
 * repeating a dish of the autofill (RepeatWindow), whether the welcome screen was already answered
 * and the version of the unit conversion already applied to the recipe book (see
 * MigrateRecipeUnitsUseCase). Implemented in :core:data.
 */
interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    val isOnboardingCompleted: Flow<Boolean>
    val repeatWindow: Flow<RepeatWindow>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setRepeatWindow(window: RepeatWindow)
    suspend fun completeOnboarding()
    suspend fun recipeUnitsVersion(): Int
    suspend fun setRecipeUnitsVersion(version: Int)
}
