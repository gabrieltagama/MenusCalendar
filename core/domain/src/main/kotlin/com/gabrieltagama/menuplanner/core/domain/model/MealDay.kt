package com.gabrieltagama.menuplanner.core.domain.model

import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import java.time.Instant
import java.time.LocalDate

/**
 * The lunch planned for one calendar day. A day has exactly one menu, which is either
 * starter + main or a single dish. Desserts are kept in the recipe book but never planned. The
 * menu knows its invariant: every dish must sit in the slot of its type (violation).
 */
data class MealDay(
    override val date: LocalDate,
    val menu: DailyMenu,
    override val updatedAt: Instant
) : PlannedDay<MealDay> {
    override fun movedTo(date: LocalDate, updatedAt: Instant): MealDay = copy(date = date, updatedAt = updatedAt)
}

sealed interface DailyMenu {
    val slots: List<Pair<MenuSlot, Dish>>

    val dishes: List<Dish> get() = slots.map { it.second }

    val violation: DomainError.WrongDishType?
        get() = slots.firstOrNull { (slot, dish) -> dish.type != slot.dishType }
            ?.let { (slot, _) -> DomainError.WrongDishType(slot, slot.dishType) }

    data class Courses(
        val starter: Dish,
        val main: Dish
    ) : DailyMenu {
        override val slots: List<Pair<MenuSlot, Dish>> get() = listOf(MenuSlot.STARTER to starter, MenuSlot.MAIN to main)
    }

    data class Single(val single: Dish) : DailyMenu {
        override val slots: List<Pair<MenuSlot, Dish>> get() = listOf(MenuSlot.SINGLE to single)
    }
}
