package com.gabrieltagama.menuplanner.core.data.database

import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests of the Room schema history with the exported schema JSON of every version
 * (core/data/schemas/...). Each migration keeps the data, leaves the tables exactly as the exported
 * schema of the next version and the fully migrated database opens with the current entities.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val database = ExportedSchemaDatabase(TEST_DB)

    @After
    fun tearDown() {
        database.delete()
    }

    @Test
    fun migrate1To2KeepsDataAndAddsRatingAndMealTimeDefaults() {
        database.create(version = 1, seed = ::insertVersion1Rows)

        database.migrateTo(2).use { db ->
            db.query("SELECT name, rating, meal_time FROM dishes WHERE id = 'd1'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Soup", cursor.getString(0))
                assertEquals("THREE", cursor.getString(1))
                assertEquals("ANY", cursor.getString(2))
            }
            assertEquals(1, db.count("SELECT COUNT(*) FROM meal_days WHERE single_id = 'd1'"))
        }
    }

    @Test
    fun migrate2To3KeepsDataAndAddsDinnerDays() {
        database.create(version = 1, seed = ::insertVersion1Rows)
        database.migrateTo(2).close()

        database.migrateTo(3).use { db ->
            db.execSQL("INSERT INTO dinner_days (date, dish_id, updated_at) VALUES (20000, 'd1', 1)")
            assertEquals(1, db.count("SELECT COUNT(*) FROM meal_days WHERE single_id = 'd1'"))
            assertEquals(1, db.count("SELECT COUNT(*) FROM dinner_days WHERE dish_id = 'd1'"))
        }
    }

    @Test
    fun migrate3To4KeepsPlannedDishesAndDropsDesserts() {
        database.create(version = 1, seed = ::insertVersion1Rows)
        database.migrateTo(3).use { db ->
            db.execSQL("INSERT INTO dishes (id, name, description, preparation, type, heaviness, rating, meal_time, updated_at) VALUES ('ds', 'Flan', '', '', 'DESSERT', 'MEDIUM', 'THREE', 'ANY', 1)")
            db.execSQL("UPDATE meal_days SET dessert_id = 'ds' WHERE date = 20000")
        }

        database.migrateTo(4).use { db ->
            assertEquals(1, db.count("SELECT COUNT(*) FROM meal_days WHERE single_id = 'd1'"))
            db.execSQL("DELETE FROM dishes WHERE id = 'ds'")
            assertEquals(0, db.count("SELECT COUNT(*) FROM dishes WHERE id = 'ds'"))
        }
    }

    @Test
    fun databaseMigratedFromVersion1OpensWithTheCurrentEntities() {
        database.create(version = 1, seed = ::insertVersion1Rows)

        val room = database.openWithRoom()

        assertEquals(1, room.openHelper.readableDatabase.count("SELECT COUNT(*) FROM meal_days WHERE single_id = 'd1'"))
        room.close()
    }

    @Test
    fun registeredMigrationsCoverEveryVersion() =
        assertEquals(listOf(1 to 2, 2 to 3, 3 to 4), Migrations.ALL.map { it.startVersion to it.endVersion })

    private fun insertVersion1Rows(db: SQLiteDatabase) {
        db.execSQL("INSERT INTO dishes (id, name, description, preparation, type, heaviness, updated_at) VALUES ('d1', 'Soup', '', '', 'STARTER', 'VERY_LOW', 1)")
        db.execSQL("INSERT INTO ingredients (dish_id, position, name, quantity, unit) VALUES ('d1', 0, 'Water', 1.0, 'LITER')")
        db.execSQL("INSERT INTO meal_days (date, starter_id, main_id, single_id, dessert_id, updated_at) VALUES (20000, NULL, NULL, 'd1', NULL, 1)")
    }

    private fun SupportSQLiteDatabase.count(sql: String): Int = query(sql).use { cursor ->
        cursor.moveToFirst()
        cursor.getInt(0)
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
