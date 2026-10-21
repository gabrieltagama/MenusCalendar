package com.gabrieltagama.menuplanner.core.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * The dinner planned for one calendar day: always a single dish (see Dish.isDinnerOption).
 */
data class DinnerDay(
    override val date: LocalDate,
    val dish: Dish,
    override val updatedAt: Instant
) : PlannedDay<DinnerDay> {
    override fun movedTo(date: LocalDate, updatedAt: Instant): DinnerDay = copy(date = date, updatedAt = updatedAt)
}
