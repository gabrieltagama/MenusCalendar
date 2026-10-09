package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.fake.FakeMealPlanRepository
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests that ClearMealDayUseCase, ObserveMonthPlanUseCase and GetMealDayUseCase delegate to the repository.
 */
class MealPlanDelegationTest {
    private val menu = DailyMenu.Single(dish(type = DishType.SINGLE))
    private val march10 = MealDay(LocalDate.of(2026, 3, 10), menu, TestDishes.OLD_INSTANT)
    private val march20 = MealDay(LocalDate.of(2026, 3, 20), menu, TestDishes.OLD_INSTANT)
    private val april1 = MealDay(LocalDate.of(2026, 4, 1), menu, TestDishes.OLD_INSTANT)
    private val repository = FakeMealPlanRepository(listOf(march20, april1, march10))

    @Test
    fun `clear removes the day of the given date`() = runBlocking {
        ClearMealDayUseCase(repository)(march10.date)

        assertEquals(listOf(march10.date), repository.cleared)
        assertNull(repository.current[march10.date])
    }

    @Test
    fun `observe month emits only days of that month`() = runBlocking {
        val days = ObserveMonthPlanUseCase(repository)(YearMonth.of(2026, 3)).first()

        assertEquals(listOf(march10, march20), days)
    }

    @Test
    fun `get day returns stored day or null`() = runBlocking {
        val useCase = GetMealDayUseCase(repository)

        assertEquals(april1, useCase(april1.date))
        assertNull(useCase(LocalDate.of(2026, 5, 5)))
    }
}
