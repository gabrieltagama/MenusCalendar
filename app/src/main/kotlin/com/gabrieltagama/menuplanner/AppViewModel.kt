package com.gabrieltagama.menuplanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrieltagama.menuplanner.core.domain.model.ThemeMode
import com.gabrieltagama.menuplanner.core.domain.query.OnboardingQueries
import com.gabrieltagama.menuplanner.core.domain.query.UserPreferencesQueries
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * App-wide state holder: exposes whether the first-start welcome screen was already answered,
 * which decides between that screen and the main shell, and the theme mode chosen in the
 * settings, which the activity applies to the whole UI. Both are null until the settings are
 * read, so nothing is drawn with a wrong theme or screen.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    onboarding: OnboardingQueries,
    preferences: UserPreferencesQueries
) : ViewModel() {

    val isOnboardingCompleted: StateFlow<Boolean?> = onboarding.isOnboardingCompleted.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val themeMode: StateFlow<ThemeMode?> = preferences.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
