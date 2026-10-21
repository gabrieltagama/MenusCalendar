package com.gabrieltagama.menuplanner.core.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.gabrieltagama.menuplanner.core.data.database.DatabaseTestData
import com.gabrieltagama.menuplanner.core.data.database.MenuPlannerDatabase
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Robolectric tests of RoomDishUsageRepository: the last day up to the limit each dish was planned
 * as starter, main or single dish at lunch or at dinner, refreshed when a calendar changes.
 */
@RunWith(AndroidJUnit4::class)
class RoomDishUsageRepositoryTest {
    private lateinit var database: MenuPlannerDatabase
    private lateinit var lunches: RoomLunchPlanRepository
    private lateinit var dinners: RoomDinnerPlanRepository
    private lateinit var repository: RoomDishUsageRepository

    private val today = LocalDate.of(2026, 3, 15)
    private val starter = DatabaseTestData.dish("st", type = DishType.STARTER)
    private val main = DatabaseTestData.dish("mn", type = DishType.MAIN)
    private val single = DatabaseTestData.dish("sg", type = DishType.SINGLE)
    private val unused = DatabaseTestData.dish("un", type = DishType.MAIN)

    @Before
    fun setUp(): Unit = runBlocking {
        database = DatabaseTestData.inMemoryDatabase()
        val dishes = RoomDishRepository(database.dishDao())
        lunches = RoomLunchPlanRepository(database.lunchDayDao(), database.dishDao())
        dinners = RoomDinnerPlanRepository(database.dinnerDayDao(), database.dishDao())
        repository = RoomDishUsageRepository(database.dishUsageDao())
        listOf(starter, main, single, unused).forEach { dishes.upsert(it) }
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun lastServedTakesTheLatestPastDayOfBothCalendars() = runTest {
        lunches.save(DatabaseTestData.coursesDay(LocalDate.of(2026, 3, 1), starter, main))
        lunches.save(DatabaseTestData.coursesDay(LocalDate.of(2026, 3, 10), starter, main))
        lunches.save(DatabaseTestData.singleDay(LocalDate.of(2026, 3, 20), single))
        dinners.save(DinnerDay(date = today, dish = main, updatedAt = DatabaseTestData.OLD_INSTANT))

        repository.observeLastServed(today).test {
            assertEquals(
                mapOf(starter.id to LocalDate.of(2026, 3, 10), main.id to today),
                awaitItem()
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun refreshesWhenACalendarChanges() = runTest {
        repository.observeLastServed(today).test {
            assertEquals(emptyMap<Any, Any>(), awaitItem())

            lunches.save(DatabaseTestData.singleDay(LocalDate.of(2026, 3, 12), single))

            assertEquals(mapOf(single.id to LocalDate.of(2026, 3, 12)), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
