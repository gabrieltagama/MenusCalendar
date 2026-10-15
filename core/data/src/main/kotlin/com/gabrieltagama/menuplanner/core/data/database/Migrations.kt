package com.gabrieltagama.menuplanner.core.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Registry of schema migrations passed to the database builder. Add one Migration per version
 * bump, e.g. a new dish field via `ALTER TABLE dishes ADD COLUMN ... NOT NULL DEFAULT ...`.
 * 1 -> 2: dish rating (THREE) and meal time (ANY), so existing dishes keep being planned.
 */
internal object Migrations {

    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `dishes` ADD COLUMN `rating` TEXT NOT NULL DEFAULT 'THREE'")
            db.execSQL("ALTER TABLE `dishes` ADD COLUMN `meal_time` TEXT NOT NULL DEFAULT 'ANY'")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
