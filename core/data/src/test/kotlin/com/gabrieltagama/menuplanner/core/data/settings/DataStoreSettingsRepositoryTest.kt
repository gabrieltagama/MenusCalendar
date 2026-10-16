package com.gabrieltagama.menuplanner.core.data.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gabrieltagama.menuplanner.core.data.di.DataStoreModule
import com.gabrieltagama.menuplanner.core.domain.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Robolectric tests of the settings DataStore: defaults on a fresh install, values written by the
 * previous SharedPreferences version are kept, and changes are read back.
 */
@RunWith(AndroidJUnit4::class)
class DataStoreSettingsRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun freshInstallUsesDefaults() = runBlocking {
        val repository = repository()

        assertEquals(ThemeMode.SYSTEM, repository.themeMode.first())
        assertFalse(repository.isOnboardingCompleted.first())
        assertEquals(0, repository.recipeUnitsVersion())
    }

    @Test
    fun valuesSavedWithSharedPreferencesAreKept() = runBlocking {
        context.getSharedPreferences("menu_planner_settings", Context.MODE_PRIVATE).edit()
            .putString("theme_mode", "DARK")
            .putBoolean("onboarding_completed", true)
            .commit()

        val repository = repository()

        assertEquals(ThemeMode.DARK, repository.themeMode.first())
        assertTrue(repository.isOnboardingCompleted.first())
    }

    @Test
    fun changesAreReadBack() = runBlocking {
        val repository = repository()

        repository.setThemeMode(ThemeMode.LIGHT)
        repository.completeOnboarding()
        repository.setRecipeUnitsVersion(3)

        assertEquals(ThemeMode.LIGHT, repository.themeMode.first())
        assertTrue(repository.isOnboardingCompleted.first())
        assertEquals(3, repository.recipeUnitsVersion())
    }

    private fun repository() = DataStoreSettingsRepository(DataStoreModule.settingsDataStore(context, scope))
}
