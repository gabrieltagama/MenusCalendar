package com.gabrieltagama.menuplanner.core.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * Content of a shared file already decoded and validated field by field, ready to be merged. Days
 * only carry dish ids because they may reference dishes arriving in the same file; resolve turns
 * them into calendar days once the dishes are known and returns null when the day breaks a
 * calendar rule: a lunch needs exactly one shape (single, or starter + main) with every dish
 * known and in a slot of its type, a dinner needs a known dish that is a dinner option.
 */
data class ImportContent(
    val dishes: List<Dish>,
    val lunches: List<ImportedLunch> = emptyList(),
    val dinners: List<ImportedDinner> = emptyList()
)

data class ImportedLunch(val date: LocalDate, val dishIds: Map<MenuSlot, String>, val updatedAt: Instant) {

    fun resolve(dishesById: Map<String, Dish>): MealDay? {
        val dishes = dishIds.mapValues { (_, id) -> dishesById[id] ?: return null }
        val menu = menuOf(dishes)?.takeIf { it.misplacedSlot == null } ?: return null
        return MealDay(date = date, menu = menu, updatedAt = updatedAt)
    }

    private fun menuOf(dishes: Map<MenuSlot, Dish>): DailyMenu? {
        val single = dishes[MenuSlot.SINGLE]
        val starter = dishes[MenuSlot.STARTER]
        val main = dishes[MenuSlot.MAIN]
        val dessert = dishes[MenuSlot.DESSERT]
        return when {
            single != null && starter == null && main == null -> DailyMenu.Single(single = single, dessert = dessert)
            single == null && starter != null && main != null -> DailyMenu.Courses(starter = starter, main = main, dessert = dessert)
            else -> null
        }
    }
}

data class ImportedDinner(val date: LocalDate, val dishId: String, val updatedAt: Instant) {

    fun resolve(dishesById: Map<String, Dish>): DinnerDay? =
        dishesById[dishId]?.takeIf(Dish::isDinnerOption)?.let { DinnerDay(date = date, dish = it, updatedAt = updatedAt) }
}
