package com.gabrieltagama.menuplanner.core.data.query

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.gabrieltagama.menuplanner.core.data.database.DatabaseTestData
import com.gabrieltagama.menuplanner.core.data.database.MenuPlannerDatabase
import com.gabrieltagama.menuplanner.core.data.repository.RoomDinnerPlanRepository
import com.gabrieltagama.menuplanner.core.data.repository.RoomDishRepository
import com.gabrieltagama.menuplanner.core.data.repository.RoomMealPlanRepository
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Robolectric tests of RoomShoppingListQueries: the ingredient lines of the dishes planned in a date
 * range, once per planned dish, in date, slot and position order, for lunch and dinner, and the flow
 * refreshing when a planned dish changes its ingredients.
 */
@RunWith(AndroidJUnit4::class)
class RoomShoppingListQueriesTest {
    private lateinit var database: MenuPlannerDatabase
    private lateinit var dishRepository: RoomDishRepository
    private lateinit var mealPlan: RoomMealPlanRepository
    private lateinit var dinnerPlan: RoomDinnerPlanRepository
    private lateinit var queries: RoomShoppingListQueries

    private val tomato = Ingredient("Tomato", 500.0, MeasureUnit.GRAM)
    private val oil = Ingredient("Oil", 30.0, MeasureUnit.MILLILITER)
    private val steak = Ingredient("Steak", 2.0, MeasureUnit.UNIT)
    private val rice = Ingredient("Rice", 400.0, MeasureUnit.GRAM)

    private val starter = DatabaseTestData.dish("st", type = DishType.STARTER, ingredients = listOf(tomato, oil))
    private val main = DatabaseTestData.dish("mn", type = DishType.MAIN, ingredients = listOf(steak))
    private val single = DatabaseTestData.dish("sg", type = DishType.SINGLE, ingredients = listOf(rice))

    private val march = LocalDate.of(2026, 3, 1)..LocalDate.of(2026, 3, 31)

    @Before
    fun setUp(): Unit = runBlocking {
        database = DatabaseTestData.inMemoryDatabase()
        dishRepository = RoomDishRepository(database.dishDao())
        mealPlan = RoomMealPlanRepository(database.mealDayDao(), database.dishDao())
        dinnerPlan = RoomDinnerPlanRepository(database.dinnerDayDao(), database.dishDao())
        queries = RoomShoppingListQueries(database.planQueryDao())
        listOf(starter, main, single).forEach { dishRepository.upsert(it) }
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun lunchLinesFollowDateSlotAndPositionOncePerPlannedDish() = runTest {
        mealPlan.saveAll(
            listOf(
                DatabaseTestData.singleDay(LocalDate.of(2026, 3, 9), single),
                DatabaseTestData.coursesDay(LocalDate.of(2026, 3, 2), starter, main),
                DatabaseTestData.singleDay(LocalDate.of(2026, 3, 5), single),
                DatabaseTestData.singleDay(LocalDate.of(2026, 4, 1), single)
            )
        )

        val lines = queries.observeIngredientLines(march, Meal.LUNCH).first()

        assertEquals(listOf(tomato, oil, steak, rice, rice), lines)
    }

    @Test
    fun dinnerLinesComeOnlyFromDinnersInRange() = runTest {
        dinnerPlan.saveAll(
            listOf(
                DinnerDay(date = LocalDate.of(2026, 3, 3), dish = main, updatedAt = DatabaseTestData.OLD_INSTANT),
                DinnerDay(date = LocalDate.of(2026, 2, 28), dish = single, updatedAt = DatabaseTestData.OLD_INSTANT)
            )
        )
        mealPlan.save(DatabaseTestData.singleDay(LocalDate.of(2026, 3, 3), single))

        val lines = queries.observeIngredientLines(march, Meal.DINNER).first()

        assertEquals(listOf(steak), lines)
    }

    @Test
    fun linesRefreshWhenPlannedDishChangesItsIngredients() = runTest {
        mealPlan.save(DatabaseTestData.singleDay(LocalDate.of(2026, 3, 9), single))
        val moreRice = rice.copy(quantity = 600.0)

        queries.observeIngredientLines(march, Meal.LUNCH).test {
            assertEquals(listOf(rice), awaitItem())

            dishRepository.upsert(single.copy(ingredients = listOf(moreRice), updatedAt = DatabaseTestData.NEW_INSTANT))

            var lines = awaitItem()
            while (lines != listOf(moreRice)) lines = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
