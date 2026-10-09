package com.gabrieltagama.menuplanner.core.domain.usecase.auth

import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.fake.FakeAuthRepository
import com.gabrieltagama.menuplanner.core.domain.model.AuthState
import com.gabrieltagama.menuplanner.core.domain.model.Credential
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests LoginUseCase, LogoutUseCase and ObserveAuthStateUseCase against a fake repository.
 */
class AuthUseCasesTest {
    private val validPin = Credential.Pin("1234")
    private val repository = FakeAuthRepository(validPin)

    @Test
    fun `valid credential returns Success`() = runBlocking {
        assertEquals(Outcome.Success(Unit), LoginUseCase(repository)(validPin))
        assertEquals(listOf<Credential>(validPin), repository.receivedCredentials)
        assertEquals(AuthState.Authenticated, ObserveAuthStateUseCase(repository)().value)
    }

    @Test
    fun `invalid credential returns InvalidCredential`() = runBlocking {
        val result = LoginUseCase(repository)(Credential.Pin("0000"))

        assertEquals(Outcome.Failure(DomainError.InvalidCredential), result)
        assertEquals(AuthState.Unauthenticated, repository.authState.value)
    }

    @Test
    fun `logout delegates to repository`() = runBlocking {
        LoginUseCase(repository)(validPin)

        LogoutUseCase(repository)()

        assertEquals(1, repository.logoutCalls)
        assertEquals(AuthState.Unauthenticated, repository.authState.value)
    }
}
