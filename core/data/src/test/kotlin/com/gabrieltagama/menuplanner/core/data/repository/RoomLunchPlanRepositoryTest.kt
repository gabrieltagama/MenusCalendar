package com.gabrieltagama.menuplanner.core.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gabrieltagama.menuplanner.core.data.database.DatabaseTestData
import com.gabrieltagama.menuplanner.core.data.database.MenuPlannerDatabase
import com.gabrieltagama.menuplanner.core.data.database.entity.LunchDayEntity
import com.gabrieltagama.menuplanner.core.data.query.RoomCalendarQueries
import com.gabrieltagama.menuplanner.core.data.query.RoomShoppingListQueries
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Robolectric tests of RoomLunchPlanRepository: saving replaces a day and reading resolves its dishes,
 * and a stored row mixing a single dish with courses is ignored by the repository and by the read
 * side. The month view is covered by RoomCalendarQueriesTest.
 */
@RunWith(AndroidJUnit4::class)
class RoomLunchPlanRepositoryTest {
    private lateinit var database: MenuPlannerDatabase
    private lateinit var dishRepository: RoomDishRepository
    private lateinit var repository: RoomLunchPlanRepository

    private val starter = DatabaseTestData.dish("st", name = "Soup", type = DishType.STARTER)
    private val main = DatabaseTestData.dish("mn", name = "Steak", type = DishType.MAIN)
    private val single = DatabaseTestData.dish("sg", name = "Lasagna", type = DishType.SINGLE)

    @Before
    fun setUp(): Unit = runBlocking {
        database = DatabaseTestData.inMemoryDatabase()
        dishRepository = RoomDishRepository(database.dishDao())
        repository = RoomLunchPlanRepository(database.lunchDayDao(), database.dishDao())
        listOf(starter, main, single).forEach { dishRepository.upsert(it) }
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun saveReplacesExistingDayAndGetDayResolvesDishes() = runTest {
        val date = LocalDate.of(2026, 3, 5)
        repository.save(DatabaseTestData.singleDay(date, single))
        val replacement = DatabaseTestData.coursesDay(date, starter, main, updatedAt = DatabaseTestData.NEW_INSTANT)

        repository.save(replacement)

        assertEquals(replacement, repository.getDay(date))
        assertNull(repository.getDay(date.plusDays(1)))
    }

    @Test
    fun rowMixingSingleDishAndCoursesIsIgnoredByEveryReader() = runTest {
        val date = LocalDate.of(2026, 3, 5)
        val rice = DatabaseTestData.dish("rc", name = "Rice", type = DishType.SINGLE, ingredients = listOf(Ingredient("Arroz", 100.0, MeasureUnit.GRAM)))
        dishRepository.upsert(rice)
        database.lunchDayDao().upsert(LunchDayEntity(date = date, starterId = starter.id.value, mainId = main.id.value, singleId = rice.id.value, updatedAt = DatabaseTestData.OLD_INSTANT))

        assertNull(repository.getDay(date))
        assertTrue(RoomCalendarQueries(database.planQueryDao()).observeLunches(YearMonth.from(date)).first().isEmpty())
        assertTrue(RoomShoppingListQueries(database.planQueryDao()).observeIngredientLines(date..date, Meal.LUNCH).first().isEmpty())
    }
}
