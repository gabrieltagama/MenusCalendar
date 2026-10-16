package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.model.RepeatWindow
import com.gabrieltagama.menuplanner.core.domain.model.ThemeMode
import com.gabrieltagama.menuplanner.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory SettingsRepository.
 */
class FakeSettingsRepository(
    initial: ThemeMode = ThemeMode.SYSTEM,
    var unitsVersion: Int = 0,
    initialRepeatWindow: RepeatWindow = RepeatWindow.DEFAULT
) : SettingsRepository {
    private val mode = MutableStateFlow(initial)
    private val window = MutableStateFlow(initialRepeatWindow)
    private val onboardingCompleted = MutableStateFlow(false)

    override val themeMode: Flow<ThemeMode> = mode

    override val repeatWindow: Flow<RepeatWindow> = window

    override val isOnboardingCompleted: Flow<Boolean> = onboardingCompleted

    override suspend fun setThemeMode(mode: ThemeMode) {
        this.mode.value = mode
    }

    override suspend fun setRepeatWindow(window: RepeatWindow) {
        this.window.value = window
    }

    override suspend fun completeOnboarding() {
        onboardingCompleted.value = true
    }

    override suspend fun recipeUnitsVersion(): Int = unitsVersion

    override suspend fun setRecipeUnitsVersion(version: Int) {
        unitsVersion = version
    }
}
