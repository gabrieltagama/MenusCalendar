package com.gabrieltagama.menuplanner.core.data.mapper

import com.gabrieltagama.menuplanner.core.data.database.dao.DinnerRow
import com.gabrieltagama.menuplanner.core.data.database.dao.IngredientLineRow
import com.gabrieltagama.menuplanner.core.data.database.dao.LunchRow
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MenuShape
import com.gabrieltagama.menuplanner.core.domain.model.MenuSlot
import com.gabrieltagama.menuplanner.core.domain.model.plan.DishSummary
import com.gabrieltagama.menuplanner.core.domain.model.plan.PlannedDinner
import com.gabrieltagama.menuplanner.core.domain.model.plan.PlannedLunch

/**
 * Maps the rows of PlanQueryDao to read models. Like LunchDayMapper, the filled dish columns of a
 * lunch must be exactly the slots of one menu shape (MenuShape.of) and every one of them must join
 * a dish; otherwise the row maps to null.
 */
internal fun LunchRow.toPlannedLunch(): PlannedLunch? = toSlots()?.let { PlannedLunch(date = date, slots = it) }

internal fun DinnerRow.toPlannedDinner(): PlannedDinner =
    PlannedDinner(date = date, dish = DishSummary(id = DishId(dishId), name = dishName, type = dishType, heaviness = dishHeaviness))

internal fun IngredientLineRow.toDomain(): Ingredient = Ingredient(name = name, quantity = quantity, unit = unit)

private fun LunchRow.toSlots(): List<Pair<MenuSlot, DishSummary>>? {
    val filled = listOfNotNull(starterId?.let { MenuSlot.STARTER }, mainId?.let { MenuSlot.MAIN }, singleId?.let { MenuSlot.SINGLE }).toSet()
    val shape = MenuShape.of(filled) ?: return null
    val slots = shape.slots.mapNotNull { slot -> summaryOf(slot)?.let { slot to it } }
    return slots.takeIf { it.size == shape.slots.size }
}

private fun LunchRow.summaryOf(slot: MenuSlot): DishSummary? = when (slot) {
    MenuSlot.STARTER -> summaryOrNull(starterId, starterName, starterType, starterHeaviness)
    MenuSlot.MAIN -> summaryOrNull(mainId, mainName, mainType, mainHeaviness)
    MenuSlot.SINGLE -> summaryOrNull(singleId, singleName, singleType, singleHeaviness)
}

private fun summaryOrNull(id: String?, name: String?, type: DishType?, heaviness: Heaviness?): DishSummary? {
    if (id == null || name == null) return null
    if (type == null || heaviness == null) return null
    return DishSummary(id = DishId(id), name = name, type = type, heaviness = heaviness)
}
