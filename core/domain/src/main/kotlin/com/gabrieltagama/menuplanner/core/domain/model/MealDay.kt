package com.gabrieltagama.menuplanner.core.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * The lunch planned for one calendar day. A day has exactly one menu, which is either
 * starter + main (+ dessert) or a single dish (+ dessert). Dessert is optional in both.
 */
data class MealDay(
    val date: LocalDate,
    val menu: DailyMenu,
    val updatedAt: Instant = Instant.now()
)

sealed interface DailyMenu {
    val dessert: Dish?
    val dishes: List<Dish>

    data class Courses(
        val starter: Dish,
        val main: Dish,
        override val dessert: Dish? = null
    ) : DailyMenu {
        override val dishes: List<Dish> get() = listOfNotNull(starter, main, dessert)
    }

    data class Single(
        val single: Dish,
        override val dessert: Dish? = null
    ) : DailyMenu {
        override val dishes: List<Dish> get() = listOfNotNull(single, dessert)
    }
}
