package com.gabrieltagama.menuplanner.core.data.database.entity

/**
 * What every planned-day row has, lunch or dinner: its epoch day and the epoch millis of its last
 * change, so the generic day DAO and the import merge can work on both tables.
 */
internal interface DayPlanRow {
    val date: Long
    val updatedAt: Long
}
