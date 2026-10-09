package com.gabrieltagama.menuplanner.core.data.database

import androidx.room.migration.Migration

/**
 * Registry of schema migrations passed to the database builder. Add one Migration per version
 * bump, e.g. a new dish field via `ALTER TABLE dishes ADD COLUMN ... NOT NULL DEFAULT ...`.
 */
internal object Migrations {
    val ALL: Array<Migration> = arrayOf()
}
