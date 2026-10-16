package com.gabrieltagama.menuplanner.core.domain.usecase.settings

import com.gabrieltagama.menuplanner.core.domain.model.RepeatWindow
import com.gabrieltagama.menuplanner.core.domain.model.ThemeMode
import com.gabrieltagama.menuplanner.core.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use cases of the user settings: observe and change the theme mode and the days without repeating
 * a dish (RepeatWindow), and know or mark that the welcome screen was answered.
 */
class ObserveThemeModeUseCase @Inject constructor(private val repository: SettingsRepository) {
    operator fun invoke(): Flow<ThemeMode> = repository.themeMode
}

class SetThemeModeUseCase @Inject constructor(private val repository: SettingsRepository) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}

class ObserveOnboardingCompletedUseCase @Inject constructor(private val repository: SettingsRepository) {
    operator fun invoke(): Flow<Boolean> = repository.isOnboardingCompleted
}

class CompleteOnboardingUseCase @Inject constructor(private val repository: SettingsRepository) {
    suspend operator fun invoke() = repository.completeOnboarding()
}

class ObserveRepeatWindowUseCase @Inject constructor(private val repository: SettingsRepository) {
    operator fun invoke(): Flow<RepeatWindow> = repository.repeatWindow
}

class SetRepeatWindowUseCase @Inject constructor(private val repository: SettingsRepository) {
    suspend operator fun invoke(days: Int) = repository.setRepeatWindow(RepeatWindow.of(days))
}
