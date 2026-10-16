package com.gabrieltagama.menuplanner.core.domain.usecase.settings

import com.gabrieltagama.menuplanner.core.domain.fake.FakeSettingsRepository
import com.gabrieltagama.menuplanner.core.domain.model.RepeatWindow
import com.gabrieltagama.menuplanner.core.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests that the settings use cases read and change the theme mode, the days without repeating a
 * dish (kept within its limits) and the onboarding flag.
 */
class SettingsUseCasesTest {
    private val repository = FakeSettingsRepository()

    @Test
    fun `theme mode follows the system by default`() = runBlocking {
        assertEquals(ThemeMode.SYSTEM, ObserveThemeModeUseCase(repository)().first())
    }

    @Test
    fun `set theme mode is observed`() = runBlocking {
        SetThemeModeUseCase(repository)(ThemeMode.LIGHT)

        assertEquals(ThemeMode.LIGHT, ObserveThemeModeUseCase(repository)().first())
    }

    @Test
    fun `onboarding is pending by default`() = runBlocking {
        assertFalse(ObserveOnboardingCompletedUseCase(repository)().first())
    }

    @Test
    fun `completed onboarding is observed`() = runBlocking {
        CompleteOnboardingUseCase(repository)()

        assertTrue(ObserveOnboardingCompletedUseCase(repository)().first())
    }

    @Test
    fun `repeat window is six days by default`() = runBlocking {
        assertEquals(RepeatWindow.DEFAULT_DAYS, ObserveRepeatWindowUseCase(repository)().first().days)
    }

    @Test
    fun `set repeat window is observed and kept within its limits`() = runBlocking {
        val setRepeatWindow = SetRepeatWindowUseCase(repository)
        val observe = ObserveRepeatWindowUseCase(repository)

        setRepeatWindow(10)
        assertEquals(10, observe().first().days)

        setRepeatWindow(RepeatWindow.MAX_DAYS + 5)
        assertEquals(RepeatWindow.MAX_DAYS, observe().first().days)

        setRepeatWindow(-1)
        assertEquals(RepeatWindow.MIN_DAYS, observe().first().days)
    }
}
