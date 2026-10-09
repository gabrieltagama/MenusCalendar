package com.gabrieltagama.menuplanner.core.domain.repository

import com.gabrieltagama.menuplanner.core.domain.model.AuthState
import com.gabrieltagama.menuplanner.core.domain.model.Credential
import kotlinx.coroutines.flow.StateFlow

/**
 * Authentication port. The current implementation validates a local PIN; a remote or
 * biometric implementation can replace it without touching the UI.
 */
interface AuthRepository {
    val authState: StateFlow<AuthState>
    suspend fun authenticate(credential: Credential): Boolean
    fun logout()
}
