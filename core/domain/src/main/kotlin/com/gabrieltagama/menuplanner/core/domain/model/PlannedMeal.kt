package com.gabrieltagama.menuplanner.core.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * A meal planned in the calendar for one day: a lunch (MealDay) or a dinner (DinnerDay). dishes
 * lists every dish served in it, desserts included.
 */
sealed interface PlannedMeal {
    val date: LocalDate
    val dishes: List<Dish>
    val updatedAt: Instant
}
