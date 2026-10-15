package com.gabrieltagama.menuplanner.core.data.repository

import com.gabrieltagama.menuplanner.core.data.fake.FakeDinnerDayDao
import com.gabrieltagama.menuplanner.core.data.fake.FakeDishDao
import com.gabrieltagama.menuplanner.core.data.fake.FakeMealDayDao
import com.gabrieltagama.menuplanner.core.data.fake.TestData
import com.gabrieltagama.menuplanner.core.data.mapper.toEntity
import com.gabrieltagama.menuplanner.core.data.mapper.toIngredientEntities
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests the shared Room calendar through its lunch and dinner subclasses: range reads across
 * months with dishes resolved, batch saves, single-day access and clearing.
 */
class RoomPlanRepositoryTest {
    private val dishDao = FakeDishDao()
    private val mealDayDao = FakeMealDayDao()
    private val dinnerDayDao = FakeDinnerDayDao()
    private val lunches = RoomMealPlanRepository(mealDayDao, dishDao)
    private val dinners = RoomDinnerPlanRepository(dinnerDayDao, dishDao)
    private val single = TestData.dish("s", type = DishType.SINGLE)
    private val main = TestData.dish("m", type = DishType.MAIN)
    private val lastOfMarch = LocalDate.of(2026, 3, 31)

    private suspend fun insert(vararg dishes: Dish) =
        dishes.forEach { dishDao.upsertWithIngredients(it.toEntity(), it.toIngredientEntities()) }

    @Test
    fun `range spans months and resolves the planned dishes`() = runTest {
        insert(single)
        val days = listOf(lastOfMarch, lastOfMarch.plusDays(1), lastOfMarch.plusDays(10))
            .map { MealDay(it, DailyMenu.Single(single), TestData.OLD_INSTANT) }
        lunches.saveAll(days)

        assertEquals(days.take(2), lunches.getRange(lastOfMarch..lastOfMarch.plusDays(1)))
    }

    @Test
    fun `dinners are saved in batch and read back one by one`() = runTest {
        insert(main)
        val dinner = DinnerDay(lastOfMarch, main, TestData.OLD_INSTANT)
        dinners.saveAll(listOf(dinner))

        assertEquals(dinner, dinners.getDay(lastOfMarch))
        dinners.clear(lastOfMarch)
        assertNull(dinners.getDay(lastOfMarch))
    }

    @Test
    fun `an empty batch writes nothing`() = runTest {
        lunches.saveAll(emptyList())

        assertEquals(emptyList<MealDay>(), lunches.getRange(lastOfMarch..lastOfMarch))
        assertEquals(0, mealDayDao.upserted.size)
    }
}
