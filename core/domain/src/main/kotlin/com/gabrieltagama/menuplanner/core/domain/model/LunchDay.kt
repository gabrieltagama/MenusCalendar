package com.gabrieltagama.menuplanner.core.domain.model

import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.common.PlanningError
import java.time.Instant
import java.time.LocalDate

/**
 * The lunch planned for one calendar day. A day has exactly one menu, which is either
 * starter + main or a single dish. Desserts are kept in the recipe book but never planned. The
 * menu knows its invariant: every dish must sit in the slot of its type (violation), and plan is
 * the factory that checks it for a menu chosen by hand or received in an import.
 */
data class LunchDay(
    override val date: LocalDate,
    val menu: DailyMenu,
    override val updatedAt: Instant
) : PlannedDay<LunchDay> {
    override fun movedTo(date: LocalDate, updatedAt: Instant): LunchDay = copy(date = date, updatedAt = updatedAt)

    companion object {
        fun plan(date: LocalDate, menu: DailyMenu, updatedAt: Instant): Outcome<LunchDay> =
            menu.violation?.let { Outcome.Failure(it) } ?: Outcome.Success(LunchDay(date = date, menu = menu, updatedAt = updatedAt))
    }
}

sealed interface DailyMenu {
    val slots: List<Pair<MenuSlot, Dish>>

    val shape: MenuShape

    val dishes: List<Dish> get() = slots.map { it.second }

    val violation: PlanningError.WrongDishType?
        get() = slots.firstOrNull { (slot, dish) -> dish.type != slot.dishType }
            ?.let { (slot, _) -> PlanningError.WrongDishType(slot, slot.dishType) }

    data class Courses(
        val starter: Dish,
        val main: Dish
    ) : DailyMenu {
        override val shape: MenuShape get() = MenuShape.COURSES

        override val slots: List<Pair<MenuSlot, Dish>> get() = listOf(MenuSlot.STARTER to starter, MenuSlot.MAIN to main)
    }

    data class Single(val single: Dish) : DailyMenu {
        override val shape: MenuShape get() = MenuShape.SINGLE

        override val slots: List<Pair<MenuSlot, Dish>> get() = listOf(MenuSlot.SINGLE to single)
    }
}
