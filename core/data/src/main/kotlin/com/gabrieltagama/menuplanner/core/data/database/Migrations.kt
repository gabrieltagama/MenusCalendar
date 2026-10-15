package com.gabrieltagama.menuplanner.core.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Registry of schema migrations passed to the database builder. Add one Migration per version
 * bump, e.g. a new dish field via `ALTER TABLE dishes ADD COLUMN ... NOT NULL DEFAULT ...`.
 * 1 -> 2: dish rating (THREE) and meal time (ANY), so existing dishes keep being planned.
 * 2 -> 3: dinner_days table, one dish per planned dinner.
 */
internal object Migrations {

    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `dishes` ADD COLUMN `rating` TEXT NOT NULL DEFAULT 'THREE'")
            db.execSQL("ALTER TABLE `dishes` ADD COLUMN `meal_time` TEXT NOT NULL DEFAULT 'ANY'")
        }
    }

    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `dinner_days` (`date` INTEGER NOT NULL, `dish_id` TEXT NOT NULL, " +
                    "`updated_at` INTEGER NOT NULL, PRIMARY KEY(`date`), FOREIGN KEY(`dish_id`) REFERENCES `dishes`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE RESTRICT )"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_dinner_days_dish_id` ON `dinner_days` (`dish_id`)")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
}
