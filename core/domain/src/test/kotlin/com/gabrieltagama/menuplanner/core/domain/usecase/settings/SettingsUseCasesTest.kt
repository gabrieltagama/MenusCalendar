package com.gabrieltagama.menuplanner.core.domain.usecase.settings

import com.gabrieltagama.menuplanner.core.domain.fake.FakeSettingsRepository
import com.gabrieltagama.menuplanner.core.domain.model.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests that the settings use cases read and change the theme mode and the onboarding flag.
 */
class SettingsUseCasesTest {
    private val repository = FakeSettingsRepository()

    @Test
    fun `theme mode follows the system by default`() =
        assertEquals(ThemeMode.SYSTEM, ObserveThemeModeUseCase(repository)().value)

    @Test
    fun `set theme mode is observed`() = runBlocking {
        SetThemeModeUseCase(repository)(ThemeMode.LIGHT)

        assertEquals(ThemeMode.LIGHT, ObserveThemeModeUseCase(repository)().value)
    }

    @Test
    fun `onboarding is pending by default`() = assertFalse(ObserveOnboardingCompletedUseCase(repository)().value)

    @Test
    fun `completed onboarding is observed`() = runBlocking {
        CompleteOnboardingUseCase(repository)()

        assertTrue(ObserveOnboardingCompletedUseCase(repository)().value)
    }
}
