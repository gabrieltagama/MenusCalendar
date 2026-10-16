package com.gabrieltagama.menuplanner.core.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gabrieltagama.menuplanner.core.data.database.DatabaseTestData
import com.gabrieltagama.menuplanner.core.data.database.MenuPlannerDatabase
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Robolectric tests of RoomMealPlanRepository: saving replaces a day and reading resolves its dishes.
 * The month view is covered by RoomCalendarQueriesTest.
 */
@RunWith(AndroidJUnit4::class)
class RoomMealPlanRepositoryTest {
    private lateinit var database: MenuPlannerDatabase
    private lateinit var dishRepository: RoomDishRepository
    private lateinit var repository: RoomMealPlanRepository

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
    fun saveReplacesExistingDayAndGetDayResolvesDishes() = runTest {
        val date = LocalDate.of(2026, 3, 5)
        repository.save(DatabaseTestData.singleDay(date, single))
        val replacement = DatabaseTestData.coursesDay(date, starter, main, updatedAt = DatabaseTestData.NEW_INSTANT)

        repository.save(replacement)

        assertEquals(replacement, repository.getDay(date))
        assertNull(repository.getDay(date.plusDays(1)))
    }
}
