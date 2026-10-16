package com.gabrieltagama.menuplanner.core.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.gabrieltagama.menuplanner.core.data.database.DatabaseTestData
import com.gabrieltagama.menuplanner.core.data.database.MenuPlannerDatabase
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Robolectric tests of RoomMealPlanRepository: month filtering, save/clear and the calendar
 * refreshing when a planned dish is edited.
 */
@RunWith(AndroidJUnit4::class)
class RoomMealPlanRepositoryTest {
    private lateinit var database: MenuPlannerDatabase
    private lateinit var dishRepository: RoomDishRepository
    private lateinit var repository: RoomMealPlanRepository

    private val march = YearMonth.of(2026, 3)
    private val starter = DatabaseTestData.dish("st", name = "Soup", type = DishType.STARTER)
    private val main = DatabaseTestData.dish("mn", name = "Steak", type = DishType.MAIN)
    private val single = DatabaseTestData.dish("sg", name = "Lasagna", type = DishType.SINGLE)

    @Before
    fun setUp(): Unit = runBlocking {
        database = DatabaseTestData.inMemoryDatabase()
        dishRepository = RoomDishRepository(database.dishDao())
        repository = RoomMealPlanRepository(database.mealDayDao(), database.dishDao())
        listOf(starter, main, single).forEach { dishRepository.upsert(it) }
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun observeMonthReturnsOnlyDaysOfThatMonthSortedByDate() = runTest {
        val firstOfMarch = DatabaseTestData.singleDay(LocalDate.of(2026, 3, 1), single)
        val lastOfMarch = DatabaseTestData.coursesDay(LocalDate.of(2026, 3, 31), starter, main)
        val midMarch = DatabaseTestData.singleDay(LocalDate.of(2026, 3, 15), single)
        listOf(
            lastOfMarch,
            DatabaseTestData.singleDay(LocalDate.of(2026, 2, 28), single),
            firstOfMarch,
            DatabaseTestData.singleDay(LocalDate.of(2026, 4, 1), single),
            midMarch
        ).forEach { repository.save(it) }

        repository.observeMonth(march).test {
            assertEquals(listOf(firstOfMarch, midMarch, lastOfMarch), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeMonthRefreshesWhenPlannedDishIsRenamed() = runTest {
        val day = DatabaseTestData.singleDay(LocalDate.of(2026, 3, 10), single)
        repository.save(day)

        repository.observeMonth(march).test {
            assertEquals(listOf(day), awaitItem())

            val renamed = single.copy(name = "Veggie lasagna", updatedAt = DatabaseTestData.NEW_INSTANT)
            dishRepository.upsert(renamed)

            val expected = listOf(day.copy(menu = DatabaseTestData.singleDay(day.date, renamed).menu))
            assertEquals(expected, awaitItemMatching(expected))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeMonthEmitsWhenDayIsSavedAndCleared() = runTest {
        val date = LocalDate.of(2026, 3, 20)
        val day = DatabaseTestData.singleDay(date, single)

        repository.observeMonth(march).test {
            assertEquals(emptyList<MealDay>(), awaitItem())

            repository.save(day)
            assertEquals(listOf(day), awaitItemMatching(listOf(day)))

            repository.clear(date)
            assertEquals(emptyList<MealDay>(), awaitItemMatching(emptyList()))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun saveReplacesExistingDayAndGetDayResolvesDishes() = runTest {
        val date = LocalDate.of(2026, 3, 5)
        repository.save(DatabaseTestData.singleDay(date, single))
        val replacement = DatabaseTestData.coursesDay(date, starter, main, updatedAt = DatabaseTestData.NEW_INSTANT)

        repository.save(replacement)

        assertEquals(replacement, repository.getDay(date))
        assertNull(repository.getDay(date.plusDays(1)))
    }

    private suspend fun ReceiveTurbine<List<MealDay>>.awaitItemMatching(expected: List<MealDay>): List<MealDay> {
        var item = awaitItem()
        while (item != expected) item = awaitItem()
        return item
    }
}
