package com.gabrieltagama.menuplanner.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.Credential
import com.gabrieltagama.menuplanner.core.domain.usecase.auth.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Login state holder. It only knows that the user types a numeric PIN, which is wrapped in a
 * [Credential.Pin]; how the credential is validated belongs to the domain/data layers.
 */
data class LoginUiState(
    val pin: String = "",
    val isLoading: Boolean = false,
    val error: DomainError? = null
) {
    val canSubmit: Boolean get() = pin.isNotEmpty() && !isLoading
}

sealed interface LoginEvent {
    data object LoggedIn : LoginEvent
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val login: LoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events: Flow<LoginEvent> = _events.receiveAsFlow()

    fun onPinChange(value: String) {
        val sanitized = value.filter(Char::isDigit).take(MAX_PIN_LENGTH)
        _uiState.update { it.copy(pin = sanitized, error = null) }
    }

    fun onSubmit() {
        val pin = _uiState.value.pin
        if (!_uiState.value.canSubmit) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val outcome = login(Credential.Pin(pin))) {
                is Outcome.Success -> onLoginSucceeded()
                is Outcome.Failure -> _uiState.update { it.copy(isLoading = false, pin = "", error = outcome.error) }
            }
        }
    }

    private suspend fun onLoginSucceeded() {
        _uiState.update { it.copy(isLoading = false) }
        _events.send(LoginEvent.LoggedIn)
    }

    companion object {
        const val MAX_PIN_LENGTH = 8
    }
}
