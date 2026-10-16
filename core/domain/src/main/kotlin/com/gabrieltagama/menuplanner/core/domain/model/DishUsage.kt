package com.gabrieltagama.menuplanner.core.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Value object: the last day (up to [today]) each dish was served at lunch or dinner. A dish that
 * is missing was never served, or is only planned in the future.
 */
data class DishUsage(val today: LocalDate, val lastServed: Map<DishId, LocalDate> = emptyMap()) {

    fun lastServedOf(id: DishId): LocalDate? = lastServed[id]

    fun daysSinceServed(id: DishId): Long? = lastServed[id]?.let { ChronoUnit.DAYS.between(it, today) }

    companion object {
        val NONE = DishUsage(today = LocalDate.MIN)
    }
}
