package com.gabrieltagama.menuplanner.feature.onboarding.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.usecase.backup.ConnectCloudAccountUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.settings.CompleteOnboardingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Welcome screen state shown only on the first start. Linking Google restores the recipes from
 * the backup before entering; continuing without an account enters straight away. Either way
 * the onboarding is completed and the screen is never shown again.
 */
data class WelcomeUiState(
    val isConnecting: Boolean = false,
    val message: WelcomeMessage? = null
)

sealed interface WelcomeMessage {
    data object ConnectionCancelled : WelcomeMessage
    data class Failed(val error: DomainError) : WelcomeMessage
}

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val connectCloudAccount: ConnectCloudAccountUseCase,
    private val completeOnboarding: CompleteOnboardingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    fun onConnectStarted() = _uiState.update { it.copy(isConnecting = true, message = null) }

    fun onGoogleAuthorization(accessToken: String?) {
        if (accessToken == null) showMessage(WelcomeMessage.ConnectionCancelled)
        else connect(accessToken)
    }

    fun onContinueWithoutAccount() {
        viewModelScope.launch { completeOnboarding() }
    }

    private fun connect(accessToken: String) {
        viewModelScope.launch {
            when (val outcome = connectCloudAccount(accessToken)) {
                is Outcome.Success -> completeOnboarding()
                is Outcome.Failure -> showMessage(WelcomeMessage.Failed(outcome.error))
            }
        }
    }

    private fun showMessage(message: WelcomeMessage) = _uiState.update { it.copy(isConnecting = false, message = message) }
}
