package com.gabrieltagama.menuplanner.core.domain.usecase.settings

import com.gabrieltagama.menuplanner.core.domain.model.RepeatWindow
import com.gabrieltagama.menuplanner.core.domain.model.ThemeMode
import com.gabrieltagama.menuplanner.core.domain.port.AppStateRepository
import com.gabrieltagama.menuplanner.core.domain.port.UserPreferencesRepository
import javax.inject.Inject

/**
 * Commands of the user settings: change the theme mode and the days without repeating a dish
 * (RepeatWindow, kept within its limits), and mark that the welcome screen was answered. Screens read
 * them through UserPreferencesQueries and OnboardingQueries.
 */
class SetThemeModeUseCase @Inject constructor(private val repository: UserPreferencesRepository) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}

class CompleteOnboardingUseCase @Inject constructor(private val repository: AppStateRepository) {
    suspend operator fun invoke() = repository.completeOnboarding()
}

class SetRepeatWindowUseCase @Inject constructor(private val repository: UserPreferencesRepository) {
    suspend operator fun invoke(days: Int) = repository.setRepeatWindow(RepeatWindow.of(days))
}
