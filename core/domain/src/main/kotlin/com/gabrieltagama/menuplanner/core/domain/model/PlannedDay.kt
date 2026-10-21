package com.gabrieltagama.menuplanner.core.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * What every planned day of the calendar has, whatever the meal (MealDay for lunch, DinnerDay for
 * dinner): its date and the time of its last change. movedTo returns the same plan on another date,
 * as a paste writes it, so the generic plan commands work on both meals.
 */
interface PlannedDay<D : PlannedDay<D>> {
    val date: LocalDate
    val updatedAt: Instant

    fun movedTo(date: LocalDate, updatedAt: Instant): D
}
