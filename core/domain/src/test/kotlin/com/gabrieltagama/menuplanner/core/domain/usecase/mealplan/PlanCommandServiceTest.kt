package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.fake.FakeDinnerPlanRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeLunchPlanRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeSettingsRepository
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.LunchDay
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import java.time.Instant
import java.time.LocalDate
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PlanCommandService routes each command to the plan of the requested meal and its results undo
 * exactly what changed.
 */
class PlanCommandServiceTest {
    private val first = LocalDate.of(2026, 3, 2)
    private val second = LocalDate.of(2026, 3, 3)
    private val single = dish(id = "s", type = DishType.SINGLE)
    private val dinnerDish = dish(id = "d", type = DishType.MAIN, mealTime = MealTime.DINNER)
    private val lunches = FakeLunchPlanRepository(listOf(LunchDay(first, DailyMenu.Single(single), Instant.EPOCH)))
    private val dinners = FakeDinnerPlanRepository(listOf(DinnerDay(first, dinnerDish, Instant.EPOCH)))
    private val dishes = FakeDishRepository(listOf(single, dinnerDish))
    private val settings = FakeSettingsRepository()
    private val clock = TestDishes.fixedClock

    private val service = PlanCommandService(
        LunchPlanCommands(
            AutoFillMonthLunchesUseCase(dishes, lunches, dinners, settings, Random(1), clock),
            ClearLunchDaysUseCase(lunches),
            RestoreLunchDaysUseCase(lunches),
            PasteLunchDaysUseCase(lunches, clock),
            UndoPasteLunchDaysUseCase(lunches)
        ),
        DinnerPlanCommands(
            AutoFillMonthDinnersUseCase(dishes, lunches, dinners, settings, Random(1), clock),
            ClearDinnerDaysUseCase(dinners),
            RestoreDinnerDaysUseCase(dinners),
            PasteDinnerDaysUseCase(dinners, clock),
            UndoPasteDinnerDaysUseCase(dinners)
        )
    )

    @Test
    fun `deleting dinners leaves lunches untouched and undo restores them`() = runBlocking {
        val change = service.delete(setOf(first), Meal.DINNER)

        assertEquals(Meal.DINNER, change.view)
        assertEquals(1, change.count)
        assertEquals(emptySet<LocalDate>(), dinners.current.keys)
        assertEquals(setOf(first), lunches.current.keys)

        service.undo(change)

        assertEquals(setOf(first), dinners.current.keys)
    }

    @Test
    fun `pasting lunches writes the lunch plan and undo removes the pasted day`() = runBlocking {
        val change = service.paste(setOf(first), Meal.LUNCH, second)

        assertEquals(Meal.LUNCH, change.view)
        assertEquals(setOf(first, second), lunches.current.keys)
        assertEquals(setOf(first), dinners.current.keys)

        service.undo(change)

        assertEquals(setOf(first), lunches.current.keys)
    }
}
