package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.fake.FakeMealPlanRepository
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.model.MenuSlot
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests slot/type validation, timestamping and persistence of SaveMealDayUseCase.
 */
class SaveMealDayUseCaseTest {
    private val date = LocalDate.of(2026, 3, 10)
    private val starter = dish(type = DishType.STARTER)
    private val main = dish(type = DishType.MAIN)
    private val single = dish(type = DishType.SINGLE)

    private lateinit var repository: FakeMealPlanRepository
    private lateinit var useCase: SaveMealDayUseCase

    @Before
    fun setUp() {
        repository = FakeMealPlanRepository()
        useCase = SaveMealDayUseCase(repository, TestDishes.fixedClock)
    }

    private fun assertRejected(menu: DailyMenu, slot: MenuSlot, expected: DishType) = runBlocking {
        assertEquals(Outcome.Failure(DomainError.WrongDishType(slot, expected)), useCase(date, menu))
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `valid courses menu is saved`() = runBlocking {
        val menu = DailyMenu.Courses(starter, main)

        val result = useCase(date, menu)

        val expected = MealDay(date, menu, TestDishes.FIXED_INSTANT)
        assertEquals(Outcome.Success(expected), result)
        assertEquals(listOf(expected), repository.saved)
    }

    @Test
    fun `courses with wrong starter fails on STARTER slot`() =
        assertRejected(DailyMenu.Courses(main, main), MenuSlot.STARTER, DishType.STARTER)

    @Test
    fun `courses with wrong main fails on MAIN slot`() =
        assertRejected(DailyMenu.Courses(starter, single), MenuSlot.MAIN, DishType.MAIN)

    @Test
    fun `valid single menu is saved`() = runBlocking {
        val menu = DailyMenu.Single(single)

        val result = useCase(date, menu)

        assertEquals(Outcome.Success(MealDay(date, menu, TestDishes.FIXED_INSTANT)), result)
        assertEquals(1, repository.saved.size)
    }

    @Test
    fun `single menu with a main dish fails on SINGLE slot`() =
        assertRejected(DailyMenu.Single(main), MenuSlot.SINGLE, DishType.SINGLE)

    @Test
    fun `updatedAt comes from injected clock`() = runBlocking {
        val day = (useCase(date, DailyMenu.Single(single)) as Outcome.Success).value

        assertEquals(TestDishes.FIXED_INSTANT, day.updatedAt)
        assertEquals(date, day.date)
    }
}
