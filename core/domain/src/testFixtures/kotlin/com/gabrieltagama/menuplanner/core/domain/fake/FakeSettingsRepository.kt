package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.model.ThemeMode
import com.gabrieltagama.menuplanner.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory SettingsRepository.
 */
class FakeSettingsRepository(initial: ThemeMode = ThemeMode.SYSTEM, var unitsVersion: Int = 0) : SettingsRepository {
    private val mode = MutableStateFlow(initial)
    private val onboardingCompleted = MutableStateFlow(false)

    override val themeMode: Flow<ThemeMode> = mode

    override val isOnboardingCompleted: Flow<Boolean> = onboardingCompleted

    override suspend fun setThemeMode(mode: ThemeMode) {
        this.mode.value = mode
    }

    override suspend fun completeOnboarding() {
        onboardingCompleted.value = true
    }

    override suspend fun recipeUnitsVersion(): Int = unitsVersion

    override suspend fun setRecipeUnitsVersion(version: Int) {
        unitsVersion = version
    }
}
