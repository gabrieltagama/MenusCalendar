package com.gabrieltagama.menuplanner.feature.auth.testing

import com.gabrieltagama.menuplanner.core.domain.model.AuthState
import com.gabrieltagama.menuplanner.core.domain.model.Credential
import com.gabrieltagama.menuplanner.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory AuthRepository that accepts a single valid credential and records every attempt.
 */
class FakeAuthRepository(private val validCredential: Credential) : AuthRepository {
    private val state = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val receivedCredentials = mutableListOf<Credential>()

    override val authState: StateFlow<AuthState> = state.asStateFlow()

    override suspend fun authenticate(credential: Credential): Boolean {
        receivedCredentials += credential
        val isValid = credential == validCredential
        if (isValid) state.value = AuthState.Authenticated
        return isValid
    }

    override fun logout() {
        state.value = AuthState.Unauthenticated
    }
}
