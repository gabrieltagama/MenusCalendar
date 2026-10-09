package com.gabrieltagama.menuplanner.core.data.auth

import com.gabrieltagama.menuplanner.core.domain.model.AuthState
import com.gabrieltagama.menuplanner.core.domain.model.Credential
import com.gabrieltagama.menuplanner.core.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory session validated with a local PIN. The session is not persisted, so every app
 * start requires the PIN again.
 */
@Singleton
internal class LocalPinAuthRepository @Inject constructor() : AuthRepository {

    private val state = MutableStateFlow<AuthState>(AuthState.Unauthenticated)

    override val authState: StateFlow<AuthState> = state.asStateFlow()

    override suspend fun authenticate(credential: Credential): Boolean {
        val valid = isValid(credential)
        if (valid) state.value = AuthState.Authenticated
        return valid
    }

    override fun logout() {
        state.value = AuthState.Unauthenticated
    }

    private fun isValid(credential: Credential): Boolean = when (credential) {
        is Credential.Pin -> PinPolicy.matches(credential.value)
    }
}
