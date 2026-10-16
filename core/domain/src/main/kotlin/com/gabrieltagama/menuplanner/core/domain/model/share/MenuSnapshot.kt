package com.gabrieltagama.menuplanner.core.domain.model.share

import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import java.time.Instant
import java.time.LocalDate

/**
 * Content exchanged when sharing or backing up: the recipe book and, depending on the scope, the
 * planned lunches and dinners. Planned days point to dishes by id, because a received day may use a
 * dish that arrives in the same snapshot; resolving an entry applies the same rules as planning it
 * by hand (shape of the menu, dish in the slot of its type, dinner option), so an entry that breaks
 * them resolves to null and is not imported.
 */
data class MenuSnapshot(
    val dishes: List<Dish>,
    val lunches: List<LunchEntry> = emptyList(),
    val dinners: List<DinnerEntry> = emptyList()
)

enum class SnapshotScope { RECIPES, EVERYTHING }

data class LunchEntry(
    val date: LocalDate,
    val starterId: DishId? = null,
    val mainId: DishId? = null,
    val singleId: DishId? = null,
    val updatedAt: Instant
) {
    fun resolve(dishesById: Map<DishId, Dish>): MealDay? =
        menuOf(dishesById)?.takeIf { it.violation == null }?.let { MealDay(date = date, menu = it, updatedAt = updatedAt) }

    private fun menuOf(dishesById: Map<DishId, Dish>): DailyMenu? = when {
        singleId != null && starterId == null && mainId == null -> dishesById[singleId]?.let(DailyMenu::Single)
        singleId == null && starterId != null && mainId != null -> {
            val starter = dishesById[starterId]
            val main = dishesById[mainId]
            if (starter != null && main != null) DailyMenu.Courses(starter = starter, main = main) else null
        }
        else -> null
    }

    companion object {
        fun of(day: MealDay): LunchEntry = when (val menu = day.menu) {
            is DailyMenu.Courses -> LunchEntry(day.date, starterId = menu.starter.id, mainId = menu.main.id, updatedAt = day.updatedAt)
            is DailyMenu.Single -> LunchEntry(day.date, singleId = menu.single.id, updatedAt = day.updatedAt)
        }
    }
}

data class DinnerEntry(
    val date: LocalDate,
    val dishId: DishId,
    val updatedAt: Instant
) {
    fun resolve(dishesById: Map<DishId, Dish>): DinnerDay? =
        dishesById[dishId]?.takeIf(Dish::isDinnerOption)?.let { DinnerDay(date = date, dish = it, updatedAt = updatedAt) }

    companion object {
        fun of(day: DinnerDay): DinnerEntry = DinnerEntry(day.date, day.dish.id, day.updatedAt)
    }
}
