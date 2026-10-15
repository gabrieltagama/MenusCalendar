package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDinnerPlanRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeMealPlanRepository
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the dinner calendar: only non-dessert dishes served at dinner can be saved, and the
 * autofill fills only empty dinners with dinner options, never the dish of that day's lunch and,
 * when there are enough dishes, none served within 6 days at lunch or dinner.
 */
class DinnerPlanUseCasesTest {
    private val month = YearMonth.of(2026, 3)
    private val date = LocalDate.of(2026, 3, 10)

    private fun save(dish: Dish) = runBlocking {
        SaveDinnerDayUseCase(FakeDinnerPlanRepository(), TestDishes.fixedClock)(date, dish)
    }

    @Test
    fun `saves a dinner or any dish of any type but dessert`() {
        listOf(DishType.STARTER, DishType.MAIN, DishType.SINGLE).forEach { type ->
            assertTrue(save(dish(type = type, mealTime = MealTime.DINNER)) is Outcome.Success)
            assertTrue(save(dish(type = type, mealTime = MealTime.ANY)) is Outcome.Success)
        }
    }

    @Test
    fun `rejects desserts and lunch only dishes`() {
        assertEquals(Outcome.Failure(DomainError.NotADinnerDish), save(dish(type = DishType.DESSERT, mealTime = MealTime.DINNER)))
        assertEquals(Outcome.Failure(DomainError.NotADinnerDish), save(dish(type = DishType.MAIN, mealTime = MealTime.LUNCH)))
    }

    private class Run(val dinners: FakeDinnerPlanRepository, val lunches: FakeMealPlanRepository, val filled: Int)

    private fun autoFill(dishes: List<Dish>, seed: Int, lunches: List<MealDay> = emptyList(), dinners: List<DinnerDay> = emptyList()): Run = runBlocking {
        val lunchRepository = FakeMealPlanRepository(lunches)
        val dinnerRepository = FakeDinnerPlanRepository(dinners)
        val useCase = AutoFillMonthDinnersUseCase(FakeDishRepository(dishes), lunchRepository, dinnerRepository, Random(seed), TestDishes.fixedClock)
        Run(dinnerRepository, lunchRepository, useCase(month).filledDays)
    }

    private val dinnerCatalog: List<Dish> =
        (1..20).map { dish(id = "dinner-$it", type = DishType.entries.filter { t -> t != DishType.DESSERT }[it % 3], mealTime = MealTime.DINNER) } +
            (1..5).map { dish(id = "any-$it", type = DishType.MAIN, mealTime = MealTime.ANY) } +
            dish(id = "lunch-only", type = DishType.MAIN, mealTime = MealTime.LUNCH) +
            dish(id = "dessert", type = DishType.DESSERT, mealTime = MealTime.DINNER)

    @Test
    fun `fills every empty dinner with dinner options only and keeps planned dinners`() {
        val planned = DinnerDay(date = date, dish = dinnerCatalog.first())
        (1..30).forEach { seed ->
            val run = autoFill(dinnerCatalog, seed, dinners = listOf(planned))

            assertEquals(month.lengthOfMonth() - 1, run.filled)
            assertEquals(planned, run.dinners.current[date])
            assertTrue(run.dinners.current.values.all { it.dish.isDinnerOption })
        }
    }

    @Test
    fun `never repeats the lunch of the day nor a dish served within 6 days`() {
        val lunchDish = dish(id = "any-1", type = DishType.MAIN, mealTime = MealTime.ANY)
        val starter = dish(id = "starter", type = DishType.STARTER, mealTime = MealTime.LUNCH)
        val lunches = (1..month.lengthOfMonth()).map { MealDay(date = month.atDay(it), menu = DailyMenu.Courses(starter = starter, main = lunchDish)) }
        (1..30).forEach { seed ->
            val run = autoFill(dinnerCatalog, seed, lunches = lunches)
            val plan = run.dinners.current

            assertTrue(plan.values.none { it.dish.id == lunchDish.id })
            plan.forEach { (first, a) ->
                plan.forEach { (second, b) ->
                    val gap = ChronoUnit.DAYS.between(first, second)
                    if (gap in 1..6) assertTrue("seed $seed repeats ${a.dish.id}", a.dish.id != b.dish.id)
                }
            }
        }
    }

    @Test
    fun `leaves dinners empty when there is no dinner option`() {
        val run = autoFill(listOf(dish(type = DishType.MAIN, mealTime = MealTime.LUNCH)), seed = 1)

        assertEquals(0, run.filled)
        assertTrue(run.dinners.current.isEmpty())
    }

    @Test
    fun `stores every filled dinner in one batch`() {
        val run = autoFill(dinnerCatalog, seed = 1)

        assertEquals(1, run.dinners.batches.size)
        assertEquals(run.filled, run.dinners.batches.single().size)
    }
}
