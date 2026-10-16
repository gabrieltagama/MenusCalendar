package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.fake.FakeShoppingChecklistRepository
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.ShoppingFilter
import com.gabrieltagama.menuplanner.core.domain.model.ShoppingItem
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Shopping checklist: a product is ticked by its key whatever its spelling, within the filter it
 * was ticked in; another range or meal choice has its own ticks; clearing affects only one filter
 * and expired filters lose their ticks.
 */
class ShoppingChecklistUseCasesTest {
    private val repository = FakeShoppingChecklistRepository()
    private val onions = ShoppingItem(name = "Cebollas", quantity = 2.0, unit = MeasureUnit.UNIT)
    private val rice = ShoppingItem(name = "Arroz", quantity = 500.0, unit = MeasureUnit.GRAM)
    private val week = ShoppingFilter(from = LocalDate.of(2026, 10, 12), to = LocalDate.of(2026, 10, 18))
    private val nextWeek = ShoppingFilter(from = LocalDate.of(2026, 10, 19), to = LocalDate.of(2026, 10, 25))

    @Test
    fun `ticks products by key whatever their spelling`() = runBlocking {
        SetShoppingItemCheckedUseCase(repository)(week, onions, true)

        val checked = ObserveShoppingChecksUseCase(repository)(week).first()
        assertTrue(onions.copy(name = "cebolla").key in checked)
    }

    @Test
    fun `ticks belong to their filter`() = runBlocking {
        SetShoppingItemCheckedUseCase(repository)(week, onions, true)
        val observe = ObserveShoppingChecksUseCase(repository)

        assertTrue(observe(nextWeek).first().isEmpty())
        assertTrue(observe(week.copy(meals = setOf(Meal.DINNER))).first().isEmpty())
        assertEquals(setOf(onions.key), observe(week).first())
    }

    @Test
    fun `unticks one product and clears only one filter`() = runBlocking {
        val setChecked = SetShoppingItemCheckedUseCase(repository)
        setChecked(week, onions, true)
        setChecked(week, rice, true)
        setChecked(nextWeek, rice, true)

        setChecked(week, onions, false)
        assertEquals(setOf(rice.key), repository.checkedByFilter.value[week])

        ClearShoppingChecksUseCase(repository)(week)
        assertTrue(repository.checkedByFilter.value[week].orEmpty().isEmpty())
        assertEquals(setOf(rice.key), repository.checkedByFilter.value[nextWeek])
    }

    @Test
    fun `filters ended before today lose their ticks`() = runBlocking {
        val setChecked = SetShoppingItemCheckedUseCase(repository)
        setChecked(week, onions, true)
        setChecked(nextWeek, rice, true)
        val today = LocalDate.of(2026, 10, 19)
        val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

        ClearExpiredShoppingChecksUseCase(repository, clock)()

        assertEquals(setOf(nextWeek), repository.checkedByFilter.value.keys)
    }
}
