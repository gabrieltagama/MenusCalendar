package com.gabrieltagama.menuplanner.core.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * The dinner planned for one calendar day: always a single dish (see Dish.isDinnerOption).
 * updatedAt defaults to EPOCH (never saved): the use cases stamp it with the injected Clock.
 */
data class DinnerDay(
    override val date: LocalDate,
    val dish: Dish,
    override val updatedAt: Instant = Instant.EPOCH
) : PlannedMeal {
    override val dishes: List<Dish> get() = listOf(dish)
}
