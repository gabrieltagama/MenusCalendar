package com.gabrieltagama.menuplanner.core.data.query

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.gabrieltagama.menuplanner.core.data.database.DatabaseTestData
import com.gabrieltagama.menuplanner.core.data.database.MenuPlannerDatabase
import com.gabrieltagama.menuplanner.core.data.repository.RoomDinnerPlanRepository
import com.gabrieltagama.menuplanner.core.data.repository.RoomDishRepository
import com.gabrieltagama.menuplanner.core.data.repository.RoomLunchPlanRepository
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.LunchDay
import com.gabrieltagama.menuplanner.core.domain.model.plan.PlannedLunch
import com.gabrieltagama.menuplanner.core.domain.model.plan.toPlannedDinner
import com.gabrieltagama.menuplanner.core.domain.model.plan.toPlannedLunch
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Robolectric tests of RoomCalendarQueries: month filtering and order, the joined read models
 * matching the planned days, and the month refreshing when a day is saved or cleared or a planned
 * dish is renamed.
 */
@RunWith(AndroidJUnit4::class)
class RoomCalendarQueriesTest {
    private lateinit var database: MenuPlannerDatabase
    private lateinit var dishRepository: RoomDishRepository
    private lateinit var lunchPlan: RoomLunchPlanRepository
    private lateinit var dinnerPlan: RoomDinnerPlanRepository
    private lateinit var queries: RoomCalendarQueries

    private val march = YearMonth.of(2026, 3)
    private val starter = DatabaseTestData.dish("st", name = "Soup", type = DishType.STARTER)
    private val main = DatabaseTestData.dish("mn", name = "Steak", type = DishType.MAIN)
    private val single = DatabaseTestData.dish("sg", name = "Lasagna", type = DishType.SINGLE)

    @Before
    fun setUp(): Unit = runBlocking {
        database = DatabaseTestData.inMemoryDatabase()
        dishRepository = RoomDishRepository(database.dishDao())
        lunchPlan = RoomLunchPlanRepository(database.lunchDayDao(), database.dishDao())
        dinnerPlan = RoomDinnerPlanRepository(database.dinnerDayDao(), database.dishDao())
        queries = RoomCalendarQueries(database.planQueryDao())
        listOf(starter, main, single).forEach { dishRepository.upsert(it) }
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun observeLunchesReturnsOnlyDaysOfThatMonthSortedByDate() = runTest {
        val firstOfMarch = DatabaseTestData.singleDay(LocalDate.of(2026, 3, 1), single)
        val lastOfMarch = DatabaseTestData.coursesDay(LocalDate.of(2026, 3, 31), starter, main)
        val midMarch = DatabaseTestData.singleDay(LocalDate.of(2026, 3, 15), single)
        listOf(
            lastOfMarch,
            DatabaseTestData.singleDay(LocalDate.of(2026, 2, 28), single),
            firstOfMarch,
            DatabaseTestData.singleDay(LocalDate.of(2026, 4, 1), single),
            midMarch
        ).forEach { lunchPlan.save(it) }

        queries.observeLunches(march).test {
            assertEquals(listOf(firstOfMarch, midMarch, lastOfMarch).map(LunchDay::toPlannedLunch), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeLunchesRefreshesWhenPlannedDishIsRenamed() = runTest {
        val day = DatabaseTestData.singleDay(LocalDate.of(2026, 3, 10), single)
        lunchPlan.save(day)

        queries.observeLunches(march).test {
            assertEquals(listOf(day.toPlannedLunch()), awaitItem())

            val renamed = single.copy(name = "Veggie lasagna", updatedAt = DatabaseTestData.NEW_INSTANT)
            dishRepository.upsert(renamed)

            val expected = listOf(DatabaseTestData.singleDay(day.date, renamed).toPlannedLunch())
            assertEquals(expected, awaitItemMatching(expected))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeLunchesEmitsWhenDayIsSavedAndCleared() = runTest {
        val date = LocalDate.of(2026, 3, 20)
        val day = DatabaseTestData.singleDay(date, single)

        queries.observeLunches(march).test {
            assertEquals(emptyList<PlannedLunch>(), awaitItem())

            lunchPlan.save(day)
            assertEquals(listOf(day.toPlannedLunch()), awaitItemMatching(listOf(day.toPlannedLunch())))

            lunchPlan.clear(date)
            assertEquals(emptyList<PlannedLunch>(), awaitItemMatching(emptyList()))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeDinnersReturnsOnlyDinnersOfThatMonthSortedByDate() = runTest {
        val late = DinnerDay(date = LocalDate.of(2026, 3, 30), dish = main, updatedAt = DatabaseTestData.OLD_INSTANT)
        val early = DinnerDay(date = LocalDate.of(2026, 3, 2), dish = single, updatedAt = DatabaseTestData.OLD_INSTANT)
        val april = DinnerDay(date = LocalDate.of(2026, 4, 2), dish = single, updatedAt = DatabaseTestData.OLD_INSTANT)
        dinnerPlan.saveAll(listOf(late, april, early))

        queries.observeDinners(march).test {
            assertEquals(listOf(early, late).map(DinnerDay::toPlannedDinner), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun <T> ReceiveTurbine<T>.awaitItemMatching(expected: T): T {
        var item = awaitItem()
        while (item != expected) item = awaitItem()
        return item
    }
}
