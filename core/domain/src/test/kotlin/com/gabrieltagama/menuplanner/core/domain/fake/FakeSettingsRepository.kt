package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.model.ThemeMode
import com.gabrieltagama.menuplanner.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * In-memory SettingsRepository.
 */
class FakeSettingsRepository(initial: ThemeMode = ThemeMode.SYSTEM) : SettingsRepository {
    private val mode = MutableStateFlow(initial)

    override val themeMode: StateFlow<ThemeMode> = mode

    override suspend fun setThemeMode(mode: ThemeMode) {
        this.mode.value = mode
    }
}
