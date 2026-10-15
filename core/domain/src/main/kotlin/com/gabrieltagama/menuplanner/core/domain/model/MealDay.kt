package com.gabrieltagama.menuplanner.core.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * The lunch planned for one calendar day. A day has exactly one menu, which is either
 * starter + main (+ dessert) or a single dish (+ dessert). Dessert is optional in both.
 * slots exposes the dish of every filled MenuSlot and misplacedSlot is the first slot holding a
 * dish of another type, the rule checked when a day is saved or imported.
 */
data class MealDay(
    override val date: LocalDate,
    val menu: DailyMenu,
    override val updatedAt: Instant = Instant.now()
) : PlannedMeal {
    override val dishes: List<Dish> get() = menu.dishes
}

sealed interface DailyMenu {
    val dessert: Dish?
    val mainDishes: List<Dish>
    val dishes: List<Dish> get() = mainDishes + listOfNotNull(dessert)
    val slots: Map<MenuSlot, Dish>

    val misplacedSlot: MenuSlot? get() = slots.entries.firstOrNull { (slot, dish) -> !slot.accepts(dish.type) }?.key

    data class Courses(
        val starter: Dish,
        val main: Dish,
        override val dessert: Dish? = null
    ) : DailyMenu {
        override val mainDishes: List<Dish> get() = listOf(starter, main)
        override val slots: Map<MenuSlot, Dish>
            get() = listOfNotNull(MenuSlot.STARTER to starter, MenuSlot.MAIN to main, dessert?.let { MenuSlot.DESSERT to it }).toMap()
    }

    data class Single(
        val single: Dish,
        override val dessert: Dish? = null
    ) : DailyMenu {
        override val mainDishes: List<Dish> get() = listOf(single)
        override val slots: Map<MenuSlot, Dish>
            get() = listOfNotNull(MenuSlot.SINGLE to single, dessert?.let { MenuSlot.DESSERT to it }).toMap()
    }
}
