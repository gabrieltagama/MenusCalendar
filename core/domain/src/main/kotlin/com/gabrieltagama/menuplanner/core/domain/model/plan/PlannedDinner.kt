package com.gabrieltagama.menuplanner.core.domain.model.plan

import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import java.time.LocalDate

/**
 * Read model of the dinner planned for one day as the calendar shows it.
 */
data class PlannedDinner(
    val date: LocalDate,
    val dish: DishSummary
)

fun DinnerDay.toPlannedDinner(): PlannedDinner = PlannedDinner(date = date, dish = dish.toSummary())
