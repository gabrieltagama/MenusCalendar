package com.gabrieltagama.menuplanner.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrieltagama.menuplanner.core.domain.model.ThemeMode
import com.gabrieltagama.menuplanner.core.domain.usecase.settings.ObserveThemeModeUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.settings.SetThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Settings screen state: exposes the chosen theme mode and saves a new choice.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeThemeMode: ObserveThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = observeThemeMode()

    fun onThemeModeSelect(mode: ThemeMode) {
        viewModelScope.launch { setThemeMode(mode) }
    }
}
