package com.gabrieltagama.menuplanner.feature.auth.ui

import app.cash.turbine.test
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.model.AuthState
import com.gabrieltagama.menuplanner.core.domain.model.Credential
import com.gabrieltagama.menuplanner.core.domain.usecase.auth.LoginUseCase
import com.gabrieltagama.menuplanner.feature.auth.testing.FakeAuthRepository
import com.gabrieltagama.menuplanner.feature.auth.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * LoginViewModel over the real LoginUseCase and an in-memory AuthRepository: PIN sanitizing,
 * wrong PIN error and the one-shot LoggedIn event.
 */
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val validPin = "1234"
    private val repository = FakeAuthRepository(Credential.Pin(validPin))
    private val viewModel by lazy { LoginViewModel(LoginUseCase(repository)) }

    @Test
    fun `initial state is empty and cannot submit`() {
        val state = viewModel.uiState.value

        assertEquals("", state.pin)
        assertFalse(state.canSubmit)
        assertNull(state.error)
    }

    @Test
    fun `non digit characters are ignored`() {
        viewModel.onPinChange("1a2 b3-4.")

        assertEquals("1234", viewModel.uiState.value.pin)
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `pin is truncated to max length`() {
        viewModel.onPinChange("1234567890")

        assertEquals("12345678", viewModel.uiState.value.pin)
        assertEquals(LoginViewModel.MAX_PIN_LENGTH, viewModel.uiState.value.pin.length)
    }

    @Test
    fun `submit with empty pin does not call repository`() = runTest {
        viewModel.onSubmit()

        assertTrue(repository.receivedCredentials.isEmpty())
    }

    @Test
    fun `wrong pin shows error and clears pin`() = runTest {
        viewModel.onPinChange("0000")

        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertEquals(DomainError.InvalidCredential, state.error)
        assertEquals("", state.pin)
        assertFalse(state.isLoading)
        assertEquals(listOf<Credential>(Credential.Pin("0000")), repository.receivedCredentials)
        assertEquals(AuthState.Unauthenticated, repository.authState.value)
    }

    @Test
    fun `typing after an error clears the error`() = runTest {
        viewModel.onPinChange("0000")
        viewModel.onSubmit()

        viewModel.onPinChange("1")

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `right pin emits logged in event`() = runTest {
        viewModel.events.test {
            viewModel.onPinChange(validPin)
            viewModel.onSubmit()

            assertEquals(LoginEvent.LoggedIn, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
        assertEquals(AuthState.Authenticated, repository.authState.value)
    }
}
