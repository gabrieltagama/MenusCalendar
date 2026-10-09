package com.gabrieltagama.menuplanner.core.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke test of the Room schema history. Requires the exported schema JSON
 * (core/data/schemas/.../1.json), which the Room Gradle plugin generates at build time and the
 * androidTest assets include. When a version is added, extend this class with
 * runMigrationsAndValidate from every previous version through Migrations.ALL.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MenuPlannerDatabase::class.java
    )

    @Test
    fun createsVersion1WithExpectedTables() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL("INSERT INTO dishes (id, name, description, preparation, type, heaviness, updated_at) VALUES ('d1', 'Soup', '', '', 'STARTER', 'VERY_LOW', 1)")
            db.execSQL("INSERT INTO ingredients (dish_id, position, name, quantity, unit) VALUES ('d1', 0, 'Water', 1.0, 'LITER')")
            db.execSQL("INSERT INTO meal_days (date, starter_id, main_id, single_id, dessert_id, updated_at) VALUES (20000, NULL, NULL, 'd1', NULL, 1)")

            assertEquals(1, db.version)
            db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name IN ('dishes', 'ingredients', 'meal_days')").use { cursor ->
                assertEquals(3, cursor.count)
            }
            db.query("SELECT COUNT(*) FROM meal_days WHERE single_id = 'd1'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
        }
    }

    @Test
    fun registeredMigrationsMatchCurrentVersion() =
        assertTrue("Version 1 needs no migrations", Migrations.ALL.isEmpty())

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
