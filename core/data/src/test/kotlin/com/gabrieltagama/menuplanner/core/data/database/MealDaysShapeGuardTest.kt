package com.gabrieltagama.menuplanner.core.data.database

import android.content.Context
import android.database.SQLException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gabrieltagama.menuplanner.core.data.database.entity.LunchDayEntity
import com.gabrieltagama.menuplanner.core.data.repository.RoomDishRepository
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Robolectric tests of MealDaysShapeGuard: opening the database deletes the rows of meal_days that
 * mix a single dish with courses and keeps the valid ones, and afterwards the triggers reject any
 * insert or update that would mix them.
 */
@RunWith(AndroidJUnit4::class)
class MealDaysShapeGuardTest {
    private lateinit var database: MenuPlannerDatabase

    private val date = LocalDate.of(2026, 3, 5)
    private val starter = DatabaseTestData.dish("st", type = DishType.STARTER)
    private val main = DatabaseTestData.dish("mn", type = DishType.MAIN)
    private val single = DatabaseTestData.dish("sg", type = DishType.SINGLE)
    private val mixed = LunchDayEntity(date = date, starterId = "st", mainId = "mn", singleId = "sg", updatedAt = DatabaseTestData.OLD_INSTANT)
    private val courses = LunchDayEntity(date = date.plusDays(1), starterId = "st", mainId = "mn", singleId = null, updatedAt = DatabaseTestData.OLD_INSTANT)

    @Before
    fun setUp(): Unit = runBlocking {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), MenuPlannerDatabase::class.java)
            .addCallback(MealDaysShapeGuard)
            .build()
        val dishes = RoomDishRepository(database.dishDao())
        listOf(starter, main, single).forEach { dishes.upsert(it) }
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun mixedRowsAreRejected() {
        assertThrows(SQLException::class.java) { runBlocking { database.lunchDayDao().upsert(mixed) } }
        runBlocking { database.lunchDayDao().upsert(courses) }
        assertThrows(SQLException::class.java) { runBlocking { database.lunchDayDao().upsert(courses.copy(singleId = "sg")) } }

        assertEquals(listOf(courses), runBlocking { database.lunchDayDao().getAll() })
    }

    @Test
    fun openingTheDatabaseDeletesMixedRows() = runBlocking {
        val db = database.openHelper.writableDatabase
        db.execSQL("DROP TRIGGER `meal_days_shape_insert`")
        database.lunchDayDao().upsert(mixed)
        database.lunchDayDao().upsert(courses)

        MealDaysShapeGuard.onOpen(db)

        assertNull(database.lunchDayDao().getByDate(date))
        assertEquals(courses, database.lunchDayDao().getByDate(courses.date))
    }
}
