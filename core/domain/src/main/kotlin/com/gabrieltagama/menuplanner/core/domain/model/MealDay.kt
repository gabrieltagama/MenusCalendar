package com.gabrieltagama.menuplanner.core.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * The lunch planned for one calendar day. A day has exactly one menu, which is either
 * starter + main or a single dish. Desserts are kept in the recipe book but never planned.
 */
data class MealDay(
    val date: LocalDate,
    val menu: DailyMenu,
    val updatedAt: Instant = Instant.now()
)

sealed interface DailyMenu {
    val dishes: List<Dish>

    data class Courses(
        val starter: Dish,
        val main: Dish
    ) : DailyMenu {
        override val dishes: List<Dish> get() = listOf(starter, main)
    }

    data class Single(val single: Dish) : DailyMenu {
        override val dishes: List<Dish> get() = listOf(single)
    }
}
