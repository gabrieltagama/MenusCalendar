package com.gabrieltagama.menuplanner.core.domain.model

import java.time.LocalDate

/**
 * What a shopping list covers: a date range (both ends included, never ending before it starts)
 * and the meals it adds up. Ticks of bought products belong to one filter, so the same product in
 * another range or meal choice starts unticked.
 */
data class ShoppingFilter(val from: LocalDate, val to: LocalDate, val meals: Set<Meal> = Meal.entries.toSet()) {
    init {
        require(!to.isBefore(from)) { "A shopping filter cannot end before it starts" }
    }

    val dates: ClosedRange<LocalDate> get() = from..to
}
