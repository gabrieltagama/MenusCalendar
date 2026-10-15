package com.gabrieltagama.menuplanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrieltagama.menuplanner.core.domain.model.ThemeMode
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.LoadInitialDishesUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.settings.ObserveOnboardingCompletedUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.settings.ObserveThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * App-wide state holder: exposes whether the first-start welcome screen was already answered,
 * which decides between that screen and the main shell, and the theme mode chosen in the
 * settings, which the activity applies to the whole UI. On creation it loads the bundled starter
 * recipe book, which only happens on the first launch after installing the app.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    observeOnboardingCompleted: ObserveOnboardingCompletedUseCase,
    observeThemeMode: ObserveThemeModeUseCase,
    loadInitialDishes: LoadInitialDishesUseCase
) : ViewModel() {

    val isOnboardingCompleted: StateFlow<Boolean> = observeOnboardingCompleted()

    val themeMode: StateFlow<ThemeMode> = observeThemeMode()

    init {
        viewModelScope.launch { loadInitialDishes() }
    }
}
