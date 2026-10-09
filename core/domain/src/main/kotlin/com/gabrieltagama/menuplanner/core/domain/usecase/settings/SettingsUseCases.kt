package com.gabrieltagama.menuplanner.core.domain.usecase.settings

import com.gabrieltagama.menuplanner.core.domain.model.ThemeMode
import com.gabrieltagama.menuplanner.core.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/**
 * Use cases of the settings screen: observe and change the theme mode.
 */
class ObserveThemeModeUseCase @Inject constructor(private val repository: SettingsRepository) {
    operator fun invoke(): StateFlow<ThemeMode> = repository.themeMode
}

class SetThemeModeUseCase @Inject constructor(private val repository: SettingsRepository) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}
