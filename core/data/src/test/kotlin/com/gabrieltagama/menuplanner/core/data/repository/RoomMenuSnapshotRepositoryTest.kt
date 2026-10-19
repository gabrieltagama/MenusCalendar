package com.gabrieltagama.menuplanner.core.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gabrieltagama.menuplanner.core.data.database.DatabaseTestData
import com.gabrieltagama.menuplanner.core.data.database.MenuPlannerDatabase
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.share.DinnerEntry
import com.gabrieltagama.menuplanner.core.domain.model.share.LunchEntry
import com.gabrieltagama.menuplanner.core.domain.model.share.MenuSnapshot
import com.gabrieltagama.menuplanner.core.domain.model.share.SnapshotScope
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Robolectric tests of RoomMenuSnapshotRepository on an in-memory database: reading and merging
 * back restores the same data, merging again skips everything, last writer wins for dishes, lunches
 * and dinners, and planned days that break the planning rules are skipped.
 */
@RunWith(AndroidJUnit4::class)
class RoomMenuSnapshotRepositoryTest {
    private lateinit var database: MenuPlannerDatabase
    private lateinit var dishRepository: RoomDishRepository
    private lateinit var mealPlanRepository: RoomMealPlanRepository
    private lateinit var dinnerPlanRepository: RoomDinnerPlanRepository
    private lateinit var repository: RoomMenuSnapshotRepository

    private val date = LocalDate.of(2026, 3, 10)
    private val starter = DatabaseTestData.dish(
        "st",
        name = "Gazpacho",
        type = DishType.STARTER,
        ingredients = listOf(Ingredient("Tomato", 500.0, MeasureUnit.GRAM), Ingredient("Oil", 30.0, MeasureUnit.MILLILITER))
    )
    private val main = DatabaseTestData.dish("mn", name = "Paella", type = DishType.MAIN, updatedAt = DatabaseTestData.NEW_INSTANT)
    private val single = DatabaseTestData.dish("sg", name = "Lasagna", type = DishType.SINGLE)
    private val dessert = DatabaseTestData.dish("ds", name = "Flan", type = DishType.DESSERT)
    private val coursesDay = DatabaseTestData.coursesDay(date, starter, main)
    private val singleDay = DatabaseTestData.singleDay(date.plusDays(1), single, updatedAt = DatabaseTestData.NEW_INSTANT)
    private val dinner = DinnerDay(date, single, DatabaseTestData.OLD_INSTANT)

    @Before
    fun setUp() {
        database = DatabaseTestData.inMemoryDatabase()
        createRepositories()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun readAndMergeIntoAnEmptyDatabaseRestoresEverything() = runTest {
        seed()
        val snapshot = repository.read(SnapshotScope.EVERYTHING)
        recreateDatabase()

        val summary = repository.merge(snapshot)

        assertEquals(ImportSummary(4, 0, 0, 3, 0, 0), summary)
        assertEquals(setOf(dessert, starter, single, main), dishRepository.getDishes().toSet())
        assertEquals(coursesDay, mealPlanRepository.getDay(coursesDay.date))
        assertEquals(singleDay, mealPlanRepository.getDay(singleDay.date))
        assertEquals(dinner, dinnerPlanRepository.getDay(dinner.date))
    }

    @Test
    fun recipesScopeLeavesTheCalendarOut() = runTest {
        seed()

        val snapshot = repository.read(SnapshotScope.RECIPES)

        assertEquals(4, snapshot.dishes.size)
        assertTrue(snapshot.lunches.isEmpty() && snapshot.dinners.isEmpty())
    }

    @Test
    fun mergingTheSameSnapshotAgainSkipsEverything() = runTest {
        seed()

        val summary = repository.merge(repository.read(SnapshotScope.EVERYTHING))

        assertEquals(ImportSummary(0, 0, 4, 0, 0, 3), summary)
    }

    @Test
    fun newerDishReplacesLocalAndOlderOrEqualDishesAreSkipped() = runTest {
        dishRepository.upsertAll(
            listOf(
                DatabaseTestData.dish("newer", name = "Local newer"),
                DatabaseTestData.dish("older", name = "Local older", updatedAt = DatabaseTestData.NEW_INSTANT),
                DatabaseTestData.dish("equal", name = "Local equal")
            )
        )
        val incoming = listOf(
            DatabaseTestData.dish("newer", name = "Remote newer", updatedAt = DatabaseTestData.NEW_INSTANT),
            DatabaseTestData.dish("older", name = "Remote older"),
            DatabaseTestData.dish("equal", name = "Remote equal")
        )

        val summary = repository.merge(MenuSnapshot(incoming))

        assertEquals(ImportSummary(0, 1, 2, 0, 0, 0), summary)
        assertEquals("Remote newer", dishRepository.getDish(DishId("newer"))?.name)
        assertEquals("Local older", dishRepository.getDish(DishId("older"))?.name)
        assertEquals("Local equal", dishRepository.getDish(DishId("equal"))?.name)
    }

    @Test
    fun dishWithAnotherIdButTheSameNameUpdatesTheLocalOneAndItsPlannedDaysFollow() = runTest {
        val local = DatabaseTestData.dish("preloaded", name = "Lentejas", type = DishType.SINGLE)
        dishRepository.upsert(local)
        val edited = DatabaseTestData.dish("exported", name = "LENTEJAS", type = DishType.SINGLE, updatedAt = DatabaseTestData.NEW_INSTANT)
        val snapshot = MenuSnapshot(
            dishes = listOf(edited),
            lunches = listOf(LunchEntry(date, singleId = edited.id, updatedAt = DatabaseTestData.OLD_INSTANT)),
            dinners = listOf(DinnerEntry(date, edited.id, DatabaseTestData.OLD_INSTANT))
        )

        val summary = repository.merge(snapshot)

        val expected = edited.copy(id = local.id)
        assertEquals(ImportSummary(0, 1, 0, 2, 0, 0), summary)
        assertEquals(listOf(expected), dishRepository.getDishes())
        assertEquals(expected, mealPlanRepository.getDay(date)?.menu?.dishes?.single())
        assertEquals(expected, dinnerPlanRepository.getDay(date)?.dish)
    }

    @Test
    fun lunchCanUseADishOfTheSameSnapshotOrAStoredOne() = runTest {
        dishRepository.upsert(main)
        val snapshot = MenuSnapshot(
            dishes = listOf(starter),
            lunches = listOf(LunchEntry(date, starterId = starter.id, mainId = main.id, updatedAt = DatabaseTestData.OLD_INSTANT))
        )

        assertEquals(ImportSummary(1, 0, 0, 1, 0, 0), repository.merge(snapshot))
        assertEquals(coursesDay, mealPlanRepository.getDay(date))
    }

    @Test
    fun lunchesBreakingThePlanningRulesAreSkipped() = runTest {
        dishRepository.upsertAll(listOf(starter, main, dessert))
        val lunches = listOf(
            LunchEntry(date, singleId = DishId("ghost"), updatedAt = DatabaseTestData.OLD_INSTANT),
            LunchEntry(date.plusDays(1), starterId = dessert.id, mainId = main.id, updatedAt = DatabaseTestData.OLD_INSTANT),
            LunchEntry(date.plusDays(2), starterId = starter.id, updatedAt = DatabaseTestData.OLD_INSTANT)
        )

        assertEquals(ImportSummary(0, 0, 0, 0, 0, 3), repository.merge(MenuSnapshot(emptyList(), lunches)))
        (0L..2L).forEach { assertNull(mealPlanRepository.getDay(date.plusDays(it))) }
    }

    @Test
    fun newerLunchUpdatesAndOlderLunchIsSkipped() = runTest {
        val other = DatabaseTestData.dish("other", type = DishType.SINGLE)
        dishRepository.upsertAll(listOf(single, other))
        mealPlanRepository.saveAll(
            listOf(
                DatabaseTestData.singleDay(date, single, updatedAt = DatabaseTestData.OLD_INSTANT),
                DatabaseTestData.singleDay(date.plusDays(1), single, updatedAt = DatabaseTestData.NEW_INSTANT)
            )
        )
        val lunches = listOf(
            LunchEntry(date, singleId = other.id, updatedAt = DatabaseTestData.NEW_INSTANT),
            LunchEntry(date.plusDays(1), singleId = other.id, updatedAt = DatabaseTestData.OLD_INSTANT)
        )

        assertEquals(ImportSummary(0, 0, 0, 0, 1, 1), repository.merge(MenuSnapshot(emptyList(), lunches)))
        assertEquals(other, mealPlanRepository.getDay(date)?.menu?.dishes?.single())
        assertEquals(single, mealPlanRepository.getDay(date.plusDays(1))?.menu?.dishes?.single())
    }

    @Test
    fun dinnersWithoutADinnerOptionAreSkipped() = runTest {
        val lunchOnly = DatabaseTestData.dish("lunch-only", type = DishType.SINGLE, mealTime = MealTime.LUNCH)
        dishRepository.upsertAll(listOf(dessert, lunchOnly, main))
        val dinners = listOf(
            DinnerEntry(date, dessert.id, DatabaseTestData.OLD_INSTANT),
            DinnerEntry(date.plusDays(1), lunchOnly.id, DatabaseTestData.OLD_INSTANT),
            DinnerEntry(date.plusDays(2), DishId("ghost"), DatabaseTestData.OLD_INSTANT),
            DinnerEntry(date.plusDays(3), main.id, DatabaseTestData.OLD_INSTANT)
        )

        assertEquals(ImportSummary(0, 0, 0, 1, 0, 3), repository.merge(MenuSnapshot(emptyList(), dinners = dinners)))
        assertEquals(main, dinnerPlanRepository.getDay(date.plusDays(3))?.dish)
    }

    private suspend fun seed() {
        dishRepository.upsertAll(listOf(starter, main, single, dessert))
        mealPlanRepository.saveAll(listOf(coursesDay, singleDay))
        dinnerPlanRepository.save(dinner)
    }

    private fun recreateDatabase() {
        database.close()
        database = DatabaseTestData.inMemoryDatabase()
        createRepositories()
    }

    private fun createRepositories() {
        dishRepository = RoomDishRepository(database.dishDao())
        mealPlanRepository = RoomMealPlanRepository(database.mealDayDao(), database.dishDao())
        dinnerPlanRepository = RoomDinnerPlanRepository(database.dinnerDayDao(), database.dishDao())
        repository = RoomMenuSnapshotRepository(database)
    }
}
