package com.gabrieltagama.menuplanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrieltagama.menuplanner.core.domain.model.ThemeMode
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.LoadInitialDishesUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.MigrateRecipeUnitsUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.settings.ObserveOnboardingCompletedUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.settings.ObserveThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * App-wide state holder: exposes whether the first-start welcome screen was already answered,
 * which decides between that screen and the main shell, and the theme mode chosen in the
 * settings, which the activity applies to the whole UI. Both are null until the settings are
 * read, so nothing is drawn with a wrong theme or screen. On creation it loads the bundled starter
 * recipe book, which only happens on the first launch after installing the app, and converts the
 * ingredients to standard units when the conversion version changed.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    observeOnboardingCompleted: ObserveOnboardingCompletedUseCase,
    observeThemeMode: ObserveThemeModeUseCase,
    loadInitialDishes: LoadInitialDishesUseCase,
    migrateRecipeUnits: MigrateRecipeUnitsUseCase
) : ViewModel() {

    val isOnboardingCompleted: StateFlow<Boolean?> = observeOnboardingCompleted().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val themeMode: StateFlow<ThemeMode?> = observeThemeMode().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            loadInitialDishes()
            migrateRecipeUnits()
        }
    }
}
