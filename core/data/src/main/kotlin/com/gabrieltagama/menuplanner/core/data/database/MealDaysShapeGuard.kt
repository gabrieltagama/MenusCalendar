package com.gabrieltagama.menuplanner.core.data.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Keeps every row of meal_days in one of the two menu shapes of the domain (MenuShape): only
 * single_id, or only starter_id and main_id. Room cannot add a CHECK to an existing table without
 * rebuilding it, so each time the database opens the rows that mix both shapes are deleted (no
 * screen, list or export uses them, because they map to no menu) and two triggers reject any insert
 * or update that would mix them again.
 */
internal object MealDaysShapeGuard : RoomDatabase.Callback() {

    private const val VALID_SHAPE =
        "(single_id IS NOT NULL AND starter_id IS NULL AND main_id IS NULL) OR " +
            "(single_id IS NULL AND starter_id IS NOT NULL AND main_id IS NOT NULL)"

    private const val VALID_NEW_SHAPE =
        "(NEW.single_id IS NOT NULL AND NEW.starter_id IS NULL AND NEW.main_id IS NULL) OR " +
            "(NEW.single_id IS NULL AND NEW.starter_id IS NOT NULL AND NEW.main_id IS NOT NULL)"

    private const val REJECT = "SELECT RAISE(ABORT, 'meal_days needs a single dish or a starter and a main');"

    override fun onOpen(db: SupportSQLiteDatabase) {
        db.execSQL("DELETE FROM `meal_days` WHERE NOT ($VALID_SHAPE)")
        db.execSQL("CREATE TRIGGER IF NOT EXISTS `meal_days_shape_insert` BEFORE INSERT ON `meal_days` WHEN NOT ($VALID_NEW_SHAPE) BEGIN $REJECT END")
        db.execSQL("CREATE TRIGGER IF NOT EXISTS `meal_days_shape_update` BEFORE UPDATE ON `meal_days` WHEN NOT ($VALID_NEW_SHAPE) BEGIN $REJECT END")
    }
}
