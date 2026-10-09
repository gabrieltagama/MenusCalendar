package com.gabrieltagama.menuplanner.core.domain.usecase.auth

import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.AuthState
import com.gabrieltagama.menuplanner.core.domain.model.Credential
import com.gabrieltagama.menuplanner.core.domain.repository.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/**
 * Authentication use cases, independent of the credential mechanism.
 */
class ObserveAuthStateUseCase @Inject constructor(private val repository: AuthRepository) {
    operator fun invoke(): StateFlow<AuthState> = repository.authState
}

class LoginUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(credential: Credential): Outcome<Unit> =
        if (repository.authenticate(credential)) Outcome.Success(Unit) else Outcome.Failure(DomainError.InvalidCredential)
}

class LogoutUseCase @Inject constructor(private val repository: AuthRepository) {
    operator fun invoke() = repository.logout()
}
