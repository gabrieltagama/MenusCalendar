package com.gabrieltagama.menuplanner.core.domain.model

/**
 * Authentication model, open to new credential types (password, passkey, biometric).
 */
sealed interface Credential {
    data class Pin(val value: String) : Credential
}

sealed interface AuthState {
    data object Unauthenticated : AuthState
    data object Authenticated : AuthState
}
