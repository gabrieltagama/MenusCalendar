package com.gabrieltagama.menuplanner.core.data.database

import android.database.sqlite.SQLiteConstraintException
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gabrieltagama.menuplanner.core.data.database.dao.DishDao
import com.gabrieltagama.menuplanner.core.data.mapper.toDomain
import com.gabrieltagama.menuplanner.core.data.mapper.toEntity
import com.gabrieltagama.menuplanner.core.data.mapper.toIngredientEntities
import com.gabrieltagama.menuplanner.core.data.repository.RoomDinnerPlanRepository
import com.gabrieltagama.menuplanner.core.data.repository.RoomDishRepository
import com.gabrieltagama.menuplanner.core.data.repository.RoomLunchPlanRepository
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.MenuSlot
import com.gabrieltagama.menuplanner.core.domain.model.PlannedUse
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Robolectric tests of DishDao and RoomDishRepository on an in-memory Room database: ingredient
 * ordering and replacement, cascade delete, and the RESTRICT constraint for planned dishes.
 */
@RunWith(AndroidJUnit4::class)
class DishDaoTest {
    private lateinit var database: MenuPlannerDatabase
    private lateinit var dishDao: DishDao
    private lateinit var dishRepository: RoomDishRepository
    private lateinit var lunchPlanRepository: RoomLunchPlanRepository

    private val date = LocalDate.of(2026, 3, 10)

    @Before
    fun setUp() {
        database = DatabaseTestData.inMemoryDatabase()
        dishDao = database.dishDao()
        dishRepository = RoomDishRepository(dishDao)
        lunchPlanRepository = RoomLunchPlanRepository(database.lunchDayDao(), dishDao)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun upsertKeepsIngredientsInEnteredOrder() = runTest {
        val ingredients = listOf(
            Ingredient("Zucchini", 2.0, MeasureUnit.UNIT),
            Ingredient("Apple", 150.0, MeasureUnit.GRAM),
            Ingredient("Milk", 0.25, MeasureUnit.LITER),
            Ingredient("Basil", 0.0, MeasureUnit.TO_TASTE)
        )
        val dish = DatabaseTestData.dish("d1", ingredients = ingredients)

        dishDao.upsertWithIngredients(dish.toEntity(), dish.toIngredientEntities())

        val stored = dishDao.getById("d1")
        assertNotNull(stored)
        assertEquals(dish, stored!!.toDomain())
        assertEquals(listOf(0, 1, 2, 3), stored.ingredients.map { it.position }.sorted())
    }

    @Test
    fun editingDishReplacesIngredients() = runTest {
        val original = DatabaseTestData.dish(
            "d1",
            ingredients = listOf(
                Ingredient("Rice", 200.0, MeasureUnit.GRAM),
                Ingredient("Water", 0.5, MeasureUnit.LITER),
                Ingredient("Salt", 1.0, MeasureUnit.PINCH)
            )
        )
        dishRepository.upsert(original)
        val edited = original.copy(
            name = "Edited",
            ingredients = listOf(Ingredient("Pasta", 250.0, MeasureUnit.GRAM), Ingredient("Rice", 100.0, MeasureUnit.GRAM)),
            updatedAt = DatabaseTestData.NEW_INSTANT
        )

        dishRepository.upsert(edited)

        assertEquals(edited, dishRepository.getDish(DishId("d1")))
        assertEquals(2, dishDao.getById("d1")!!.ingredients.size)
        assertEquals(1, dishDao.getAll().size)
    }

    @Test
    fun editingDishToNoIngredientsRemovesAllLines() = runTest {
        val original = DatabaseTestData.dish("d1", ingredients = listOf(Ingredient("Rice", 200.0, MeasureUnit.GRAM)))
        dishRepository.upsert(original)

        dishRepository.upsert(original.copy(ingredients = emptyList()))

        assertTrue(dishDao.getById("d1")!!.ingredients.isEmpty())
    }

    @Test
    fun editingPlannedDishKeepsTheLunchDay() = runTest {
        val single = DatabaseTestData.dish("s", type = DishType.SINGLE, ingredients = listOf(Ingredient("Egg", 2.0, MeasureUnit.UNIT)))
        dishRepository.upsert(single)
        lunchPlanRepository.save(DatabaseTestData.singleDay(date, single))

        dishRepository.upsert(single.copy(name = "Renamed", updatedAt = DatabaseTestData.NEW_INSTANT))

        assertEquals("Renamed", (lunchPlanRepository.getDay(date)!!.menu.dishes.single()).name)
    }

    @Test
    fun deletingUnplannedDishCascadesIngredients() = runTest {
        dishRepository.upsert(DatabaseTestData.dish("d1", ingredients = listOf(Ingredient("Rice", 1.0, MeasureUnit.CUP))))

        assertTrue(dishRepository.deleteIfUnused(DishId("d1")))

        assertNull(dishRepository.getDish(DishId("d1")))
        assertEquals(0, countIngredientRows("d1"))
    }

    @Test
    fun plannedDishIsNotDeleted() = runTest {
        val starter = DatabaseTestData.dish("st", type = DishType.STARTER)
        val main = DatabaseTestData.dish("mn", type = DishType.MAIN)
        listOf(starter, main).forEach { dishRepository.upsert(it) }
        lunchPlanRepository.save(DatabaseTestData.coursesDay(date, starter, main))

        listOf(starter, main).forEach { assertPlannedDishCannotBeDeleted(it) }
    }

    @Test
    fun plannedDishCannotBeDeletedBypassingTheCheck() = runTest {
        val single = DatabaseTestData.dish("s", type = DishType.SINGLE)
        dishRepository.upsert(single)
        lunchPlanRepository.save(DatabaseTestData.singleDay(date, single))

        val error = runCatching { dishDao.deleteById(single.id.value) }.exceptionOrNull()

        assertTrue("Expected SQLiteConstraintException but was $error", error is SQLiteConstraintException)
        assertEquals(single, dishRepository.getDish(single.id))
    }

    @Test
    fun plannedUsesListTheLunchSlotsAndDinnersOfTheDish() = runTest {
        val starter = DatabaseTestData.dish("st", type = DishType.STARTER)
        val main = DatabaseTestData.dish("mn", type = DishType.MAIN)
        val single = DatabaseTestData.dish("sg", type = DishType.SINGLE)
        dishRepository.upsertAll(listOf(starter, main, single))
        lunchPlanRepository.saveAll(listOf(DatabaseTestData.coursesDay(date, starter, main), DatabaseTestData.singleDay(date.plusDays(1), single)))
        RoomDinnerPlanRepository(database.dinnerDayDao(), dishDao).save(DinnerDay(date.plusDays(2), single, DatabaseTestData.OLD_INSTANT))

        assertEquals(listOf(PlannedUse.Lunch(date, MenuSlot.MAIN)), dishRepository.getPlannedUses(main.id))
        assertEquals(
            setOf(PlannedUse.Lunch(date.plusDays(1), MenuSlot.SINGLE), PlannedUse.Dinner(date.plusDays(2))),
            dishRepository.getPlannedUses(single.id).toSet()
        )
        assertEquals(emptyList<PlannedUse>(), dishRepository.getPlannedUses(DishId("unplanned")))
    }

    private suspend fun assertPlannedDishCannotBeDeleted(dish: Dish) {
        assertFalse(dishRepository.deleteIfUnused(dish.id))
        assertEquals(dish, dishRepository.getDish(dish.id))
    }

    private fun countIngredientRows(dishId: String): Int =
        database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM ingredients WHERE dish_id = ?", arrayOf<Any?>(dishId)).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
}
