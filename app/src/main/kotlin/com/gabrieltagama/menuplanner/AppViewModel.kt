package com.gabrieltagama.menuplanner

import androidx.lifecycle.ViewModel
import com.gabrieltagama.menuplanner.core.domain.model.AuthState
import com.gabrieltagama.menuplanner.core.domain.model.ThemeMode
import com.gabrieltagama.menuplanner.core.domain.usecase.auth.LogoutUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.auth.ObserveAuthStateUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.settings.ObserveThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/**
 * App-wide state holder: exposes the authentication state, which is the single source of truth
 * deciding between the login flow and the main shell, performs logout and exposes the theme mode
 * chosen in the settings, which the activity applies to the whole UI.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    observeAuthState: ObserveAuthStateUseCase,
    observeThemeMode: ObserveThemeModeUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    val authState: StateFlow<AuthState> = observeAuthState()

    val themeMode: StateFlow<ThemeMode> = observeThemeMode()

    fun logout() = logoutUseCase()
}
