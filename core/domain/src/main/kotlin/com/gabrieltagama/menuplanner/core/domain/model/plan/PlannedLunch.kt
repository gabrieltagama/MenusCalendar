package com.gabrieltagama.menuplanner.core.domain.model.plan

import com.gabrieltagama.menuplanner.core.domain.model.LunchDay
import com.gabrieltagama.menuplanner.core.domain.model.MenuSlot
import java.time.LocalDate

/**
 * Read model of the lunch planned for one day as the calendar shows it: its slots (starter and
 * main, or single dish) in menu order, each with a DishSummary.
 */
data class PlannedLunch(
    val date: LocalDate,
    val slots: List<Pair<MenuSlot, DishSummary>>
) {
    val dishes: List<DishSummary> get() = slots.map { it.second }
}

fun LunchDay.toPlannedLunch(): PlannedLunch =
    PlannedLunch(date = date, slots = menu.slots.map { (slot, dish) -> slot to dish.toSummary() })
