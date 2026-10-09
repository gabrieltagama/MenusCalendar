package com.gabrieltagama.menuplanner.core.data.auth

import app.cash.turbine.test
import com.gabrieltagama.menuplanner.core.domain.model.AuthState
import com.gabrieltagama.menuplanner.core.domain.model.Credential
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the authentication state transitions of LocalPinAuthRepository.
 */
class LocalPinAuthRepositoryTest {
    private val repository = LocalPinAuthRepository()

    @Test
    fun `starts unauthenticated`() = assertEquals(AuthState.Unauthenticated, repository.authState.value)

    @Test
    fun `valid PIN authenticates`() = runTest {
        assertTrue(repository.authenticate(Credential.Pin("1234")))
        assertEquals(AuthState.Authenticated, repository.authState.value)
    }

    @Test
    fun `invalid PIN keeps unauthenticated`() = runTest {
        assertFalse(repository.authenticate(Credential.Pin("0000")))
        assertEquals(AuthState.Unauthenticated, repository.authState.value)
    }

    @Test
    fun `invalid PIN after login keeps the session`() = runTest {
        repository.authenticate(Credential.Pin("1234"))

        assertFalse(repository.authenticate(Credential.Pin("9999")))
        assertEquals(AuthState.Authenticated, repository.authState.value)
    }

    @Test
    fun `logout returns to unauthenticated`() = runTest {
        repository.authenticate(Credential.Pin("1234"))

        repository.logout()

        assertEquals(AuthState.Unauthenticated, repository.authState.value)
    }

    @Test
    fun `state flow emits every transition`() = runTest {
        repository.authState.test {
            assertEquals(AuthState.Unauthenticated, awaitItem())

            repository.authenticate(Credential.Pin(""))
            repository.authenticate(Credential.Pin("1234"))
            assertEquals(AuthState.Authenticated, awaitItem())

            repository.logout()
            assertEquals(AuthState.Unauthenticated, awaitItem())
            expectNoEvents()
        }
    }
}
