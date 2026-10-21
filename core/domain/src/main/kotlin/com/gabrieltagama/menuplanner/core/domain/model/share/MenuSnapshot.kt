package com.gabrieltagama.menuplanner.core.domain.model.share

import com.gabrieltagama.menuplanner.core.domain.common.getOrNull
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.LunchDay
import com.gabrieltagama.menuplanner.core.domain.model.MenuSlot
import com.gabrieltagama.menuplanner.core.domain.model.PlannedDay
import java.time.Instant
import java.time.LocalDate

/**
 * Content exchanged when sharing or backing up: the recipe book and, depending on the scope, the
 * planned lunches and dinners. Planned days point to dishes by id, because a received day may use a
 * dish that arrives in the same snapshot; resolving an entry applies the same rules as planning it
 * by hand (shape of the menu, dish in the slot of its type, dinner option), so an entry that breaks
 * them resolves to null and is not imported. remapped translates those ids with the idMap of a
 * DishMergePlan when an incoming dish was matched to a local one with another id. PlanEntry is what
 * lunch and dinner entries share, so an import merges both the same way. limitedTo keeps only what a
 * scope covers, so a file with more content (an old backup with calendar) imports just that part.
 */
data class MenuSnapshot(
    val dishes: List<Dish>,
    val lunches: List<LunchEntry> = emptyList(),
    val dinners: List<DinnerEntry> = emptyList()
) {
    fun limitedTo(scope: SnapshotScope): MenuSnapshot = if (scope == SnapshotScope.EVERYTHING) this else MenuSnapshot(dishes)
}

enum class SnapshotScope { RECIPES, EVERYTHING }

interface PlanEntry<D : PlannedDay<D>> {
    val date: LocalDate
    val updatedAt: Instant

    fun resolve(dishesById: Map<DishId, Dish>): D?
}

data class LunchEntry(
    override val date: LocalDate,
    val starterId: DishId? = null,
    val mainId: DishId? = null,
    val singleId: DishId? = null,
    override val updatedAt: Instant
) : PlanEntry<LunchDay> {
    fun remapped(idMap: Map<DishId, DishId>): LunchEntry = copy(
        starterId = starterId?.let { idMap[it] ?: it },
        mainId = mainId?.let { idMap[it] ?: it },
        singleId = singleId?.let { idMap[it] ?: it }
    )

    val slotIds: Map<MenuSlot, DishId>
        get() = listOfNotNull(starterId?.let { MenuSlot.STARTER to it }, mainId?.let { MenuSlot.MAIN to it }, singleId?.let { MenuSlot.SINGLE to it }).toMap()

    override fun resolve(dishesById: Map<DishId, Dish>): LunchDay? =
        menuOf(dishesById)?.let { LunchDay.plan(date, it, updatedAt).getOrNull() }

    private fun menuOf(dishesById: Map<DishId, Dish>): DailyMenu? =
        DailyMenu.fromSlots(slotIds.mapValues { (_, id) -> dishesById[id] ?: return null })

    companion object {
        fun of(day: LunchDay): LunchEntry = when (val menu = day.menu) {
            is DailyMenu.Courses -> LunchEntry(day.date, starterId = menu.starter.id, mainId = menu.main.id, updatedAt = day.updatedAt)
            is DailyMenu.Single -> LunchEntry(day.date, singleId = menu.single.id, updatedAt = day.updatedAt)
        }
    }
}

data class DinnerEntry(
    override val date: LocalDate,
    val dishId: DishId,
    override val updatedAt: Instant
) : PlanEntry<DinnerDay> {
    fun remapped(idMap: Map<DishId, DishId>): DinnerEntry = copy(dishId = idMap[dishId] ?: dishId)

    override fun resolve(dishesById: Map<DishId, Dish>): DinnerDay? =
        dishesById[dishId]?.let { DinnerDay.plan(date, it, updatedAt).getOrNull() }

    companion object {
        fun of(day: DinnerDay): DinnerEntry = DinnerEntry(day.date, day.dish.id, day.updatedAt)
    }
}
