package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.model.AuthState
import com.gabrieltagama.menuplanner.core.domain.model.Credential
import com.gabrieltagama.menuplanner.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * In-memory AuthRepository that accepts a single valid credential.
 */
class FakeAuthRepository(private val validCredential: Credential) : AuthRepository {
    private val state = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val receivedCredentials = mutableListOf<Credential>()
    var logoutCalls = 0
        private set

    override val authState: StateFlow<AuthState> = state

    override suspend fun authenticate(credential: Credential): Boolean {
        receivedCredentials += credential
        val valid = credential == validCredential
        if (valid) state.value = AuthState.Authenticated
        return valid
    }

    override fun logout() {
        logoutCalls++
        state.value = AuthState.Unauthenticated
    }
}
