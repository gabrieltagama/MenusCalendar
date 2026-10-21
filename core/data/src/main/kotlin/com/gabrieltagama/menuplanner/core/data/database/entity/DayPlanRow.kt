package com.gabrieltagama.menuplanner.core.data.database.entity

import java.time.Instant
import java.time.LocalDate

/**
 * What every planned-day row has, lunch or dinner: its date and the time of its last change
 * (stored as epoch day and epoch millis, see Converters), so the generic day DAO and the import
 * merge can work on both tables.
 */
internal interface DayPlanRow {
    val date: LocalDate
    val updatedAt: Instant
}
