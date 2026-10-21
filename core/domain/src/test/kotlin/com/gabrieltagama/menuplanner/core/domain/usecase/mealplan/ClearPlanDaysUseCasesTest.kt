package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.fake.FakeDinnerPlanRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeLunchPlanRepository
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.LunchDay
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Bulk deletion of lunches and dinners: only the given planned dates are removed, the removed days
 * are returned, and restoring them puts the plan back as it was.
 */
class ClearPlanDaysUseCasesTest {
    private val first = LocalDate.of(2026, 3, 2)
    private val second = LocalDate.of(2026, 3, 5)
    private val untouched = LocalDate.of(2026, 3, 3)
    private val single = DailyMenu.Single(single = dish(type = DishType.SINGLE))
    private val lunches = listOf(first, second, untouched).map { LunchDay(date = it, menu = single, updatedAt = Instant.EPOCH) }
    private val dinners = listOf(first, second, untouched).map { DinnerDay(date = it, dish = dish(type = DishType.MAIN, mealTime = MealTime.DINNER), updatedAt = Instant.EPOCH) }

    @Test
    fun `clears only the selected lunches and returns them`() = runBlocking {
        val repository = FakeLunchPlanRepository(lunches)

        val removed = ClearLunchDaysUseCase(repository)(setOf(first, second, LocalDate.of(2026, 3, 20)))

        assertEquals(listOf(first, second), removed.map(LunchDay::date))
        assertEquals(setOf(untouched), repository.current.keys)
    }

    @Test
    fun `restoring cleared lunches brings them back`() = runBlocking {
        val repository = FakeLunchPlanRepository(lunches)
        val removed = ClearLunchDaysUseCase(repository)(setOf(first, second))

        RestoreLunchDaysUseCase(repository)(removed)

        assertEquals(lunches.associateBy(LunchDay::date), repository.current)
    }

    @Test
    fun `clearing nothing touches nothing`() = runBlocking {
        val repository = FakeLunchPlanRepository(lunches)

        assertTrue(ClearLunchDaysUseCase(repository)(emptySet()).isEmpty())
        assertTrue(repository.cleared.isEmpty())
    }

    @Test
    fun `clears and restores selected dinners`() = runBlocking {
        val repository = FakeDinnerPlanRepository(dinners)

        val removed = ClearDinnerDaysUseCase(repository)(setOf(first, second))
        assertEquals(setOf(untouched), repository.current.keys)

        RestoreDinnerDaysUseCase(repository)(removed)
        assertEquals(dinners.associateBy(DinnerDay::date), repository.current)
    }
}
