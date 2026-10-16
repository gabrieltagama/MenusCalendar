package com.gabrieltagama.menuplanner.core.data.shopping

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gabrieltagama.menuplanner.core.data.di.DataStoreModule
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.model.ShoppingFilter
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Robolectric tests of the shopping checklist DataStore: ticks are kept per filter (dates and
 * meals, whatever the order of the meals), clearing affects one filter and expired filters are
 * dropped.
 */
@RunWith(AndroidJUnit4::class)
class DataStoreShoppingChecklistRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val week = ShoppingFilter(from = LocalDate.of(2026, 10, 12), to = LocalDate.of(2026, 10, 18))
    private val nextWeek = ShoppingFilter(from = LocalDate.of(2026, 10, 19), to = LocalDate.of(2026, 10, 25))

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun ticksAreKeptPerFilter() = runBlocking {
        val repository = repository()

        repository.setChecked(week, "cebolla", true)
        repository.setChecked(week.copy(meals = setOf(Meal.LUNCH)), "arroz", true)

        assertEquals(setOf("cebolla"), repository.checked(week.copy(meals = setOf(Meal.DINNER, Meal.LUNCH))).first())
        assertEquals(setOf("arroz"), repository.checked(week.copy(meals = setOf(Meal.LUNCH))).first())
        assertTrue(repository.checked(nextWeek).first().isEmpty())
    }

    @Test
    fun clearAffectsOnlyOneFilter() = runBlocking {
        val repository = repository()
        repository.setChecked(week, "cebolla", true)
        repository.setChecked(nextWeek, "arroz", true)

        repository.clear(week)

        assertTrue(repository.checked(week).first().isEmpty())
        assertEquals(setOf("arroz"), repository.checked(nextWeek).first())
    }

    @Test
    fun expiredFiltersAreDropped() = runBlocking {
        val repository = repository()
        repository.setChecked(week, "cebolla", true)
        repository.setChecked(nextWeek, "arroz", true)

        repository.clearEndedBefore(LocalDate.of(2026, 10, 19))

        assertTrue(repository.checked(week).first().isEmpty())
        assertEquals(setOf("arroz"), repository.checked(nextWeek).first())
    }

    private fun repository() = DataStoreShoppingChecklistRepository(DataStoreModule.settingsDataStore(context, scope))
}
