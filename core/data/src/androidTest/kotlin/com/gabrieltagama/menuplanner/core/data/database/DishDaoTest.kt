package com.gabrieltagama.menuplanner.core.data.database

import android.database.sqlite.SQLiteConstraintException
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gabrieltagama.menuplanner.core.data.database.dao.DishDao
import com.gabrieltagama.menuplanner.core.data.mapper.toDomain
import com.gabrieltagama.menuplanner.core.data.mapper.toEntity
import com.gabrieltagama.menuplanner.core.data.mapper.toIngredientEntities
import com.gabrieltagama.menuplanner.core.data.repository.RoomDishRepository
import com.gabrieltagama.menuplanner.core.data.repository.RoomMealPlanRepository
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
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
 * Instrumented tests of DishDao and RoomDishRepository on an in-memory Room database: ingredient
 * ordering and replacement, cascade delete, and the RESTRICT constraint for planned dishes.
 */
@RunWith(AndroidJUnit4::class)
class DishDaoTest {
    private lateinit var database: MenuPlannerDatabase
    private lateinit var dishDao: DishDao
    private lateinit var dishRepository: RoomDishRepository
    private lateinit var mealPlanRepository: RoomMealPlanRepository

    private val date = LocalDate.of(2026, 3, 10)

    @Before
    fun setUp() {
        database = AndroidTestData.inMemoryDatabase()
        dishDao = database.dishDao()
        dishRepository = RoomDishRepository(dishDao)
        mealPlanRepository = RoomMealPlanRepository(database.mealDayDao(), dishDao)
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
        val dish = AndroidTestData.dish("d1", ingredients = ingredients)

        dishDao.upsertWithIngredients(dish.toEntity(), dish.toIngredientEntities())

        val stored = dishDao.getById("d1")
        assertNotNull(stored)
        assertEquals(dish, stored!!.toDomain())
        assertEquals(listOf(0, 1, 2, 3), stored.ingredients.map { it.position }.sorted())
    }

    @Test
    fun editingDishReplacesIngredients() = runTest {
        val original = AndroidTestData.dish(
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
            updatedAt = AndroidTestData.NEW_INSTANT
        )

        dishRepository.upsert(edited)

        assertEquals(edited, dishRepository.getDish("d1"))
        assertEquals(2, dishDao.getById("d1")!!.ingredients.size)
        assertEquals(1, dishDao.getAll().size)
    }

    @Test
    fun editingDishToNoIngredientsRemovesAllLines() = runTest {
        val original = AndroidTestData.dish("d1", ingredients = listOf(Ingredient("Rice", 200.0, MeasureUnit.GRAM)))
        dishRepository.upsert(original)

        dishRepository.upsert(original.copy(ingredients = emptyList()))

        assertTrue(dishDao.getById("d1")!!.ingredients.isEmpty())
    }

    @Test
    fun editingPlannedDishKeepsTheMealDay() = runTest {
        val single = AndroidTestData.dish("s", type = DishType.SINGLE, ingredients = listOf(Ingredient("Egg", 2.0, MeasureUnit.UNIT)))
        dishRepository.upsert(single)
        mealPlanRepository.save(AndroidTestData.singleDay(date, single))

        dishRepository.upsert(single.copy(name = "Renamed", updatedAt = AndroidTestData.NEW_INSTANT))

        assertEquals("Renamed", (mealPlanRepository.getDay(date)!!.menu.dishes.single()).name)
    }

    @Test
    fun deletingUnplannedDishCascadesIngredients() = runTest {
        dishRepository.upsert(AndroidTestData.dish("d1", ingredients = listOf(Ingredient("Rice", 1.0, MeasureUnit.CUP))))

        assertFalse(dishRepository.isUsedInCalendar("d1"))
        dishRepository.delete("d1")

        assertNull(dishRepository.getDish("d1"))
        assertEquals(0, countIngredientRows("d1"))
    }

    @Test
    fun deletingPlannedDishFailsWithConstraintViolation() = runTest {
        val starter = AndroidTestData.dish("st", type = DishType.STARTER)
        val main = AndroidTestData.dish("mn", type = DishType.MAIN)
        val dessert = AndroidTestData.dish("ds", type = DishType.DESSERT)
        listOf(starter, main, dessert).forEach { dishRepository.upsert(it) }
        mealPlanRepository.save(AndroidTestData.coursesDay(date, starter, main, dessert))

        listOf(starter, main, dessert).forEach { assertPlannedDishCannotBeDeleted(it) }
    }

    private suspend fun assertPlannedDishCannotBeDeleted(dish: Dish) {
        assertTrue(dishRepository.isUsedInCalendar(dish.id))

        val error = runCatching { dishRepository.delete(dish.id) }.exceptionOrNull()

        assertTrue("Expected SQLiteConstraintException but was $error", error is SQLiteConstraintException)
        assertEquals(dish, dishRepository.getDish(dish.id))
    }

    private fun countIngredientRows(dishId: String): Int =
        database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM ingredients WHERE dish_id = ?", arrayOf<Any?>(dishId)).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
}
