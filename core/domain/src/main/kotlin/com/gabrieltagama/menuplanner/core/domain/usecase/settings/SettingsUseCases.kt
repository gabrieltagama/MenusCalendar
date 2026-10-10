package com.gabrieltagama.menuplanner.core.domain.usecase.settings

import com.gabrieltagama.menuplanner.core.domain.model.ThemeMode
import com.gabrieltagama.menuplanner.core.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/**
 * Use cases of the user settings: observe and change the theme mode, and know or mark that the
 * welcome screen was answered.
 */
class ObserveThemeModeUseCase @Inject constructor(private val repository: SettingsRepository) {
    operator fun invoke(): StateFlow<ThemeMode> = repository.themeMode
}

class SetThemeModeUseCase @Inject constructor(private val repository: SettingsRepository) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}

class ObserveOnboardingCompletedUseCase @Inject constructor(private val repository: SettingsRepository) {
    operator fun invoke(): StateFlow<Boolean> = repository.isOnboardingCompleted
}

class CompleteOnboardingUseCase @Inject constructor(private val repository: SettingsRepository) {
    suspend operator fun invoke() = repository.completeOnboarding()
}
