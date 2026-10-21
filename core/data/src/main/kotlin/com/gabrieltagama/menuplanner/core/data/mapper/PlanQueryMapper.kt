package com.gabrieltagama.menuplanner.core.data.mapper

import com.gabrieltagama.menuplanner.core.data.database.dao.DinnerRow
import com.gabrieltagama.menuplanner.core.data.database.dao.IngredientLineRow
import com.gabrieltagama.menuplanner.core.data.database.dao.LunchRow
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MenuSlot
import com.gabrieltagama.menuplanner.core.domain.model.plan.DishSummary
import com.gabrieltagama.menuplanner.core.domain.model.plan.PlannedDinner
import com.gabrieltagama.menuplanner.core.domain.model.plan.PlannedLunch

/**
 * Maps the rows of PlanQueryDao to read models. Like LunchDayMapper, a lunch with single_id is a
 * single dish, otherwise it needs both starter and main; an incomplete row maps to null.
 */
internal fun LunchRow.toPlannedLunch(): PlannedLunch? = toSlots()?.let { PlannedLunch(date = date, slots = it) }

internal fun DinnerRow.toPlannedDinner(): PlannedDinner =
    PlannedDinner(date = date, dish = DishSummary(id = DishId(dishId), name = dishName, type = dishType, heaviness = dishHeaviness))

internal fun IngredientLineRow.toDomain(): Ingredient = Ingredient(name = name, quantity = quantity, unit = unit)

private fun LunchRow.toSlots(): List<Pair<MenuSlot, DishSummary>>? {
    if (singleId != null) return single()?.let { listOf(MenuSlot.SINGLE to it) }
    val starter = starter() ?: return null
    val main = main() ?: return null
    return listOf(MenuSlot.STARTER to starter, MenuSlot.MAIN to main)
}

private fun LunchRow.starter(): DishSummary? = summaryOrNull(starterId, starterName, starterType, starterHeaviness)

private fun LunchRow.main(): DishSummary? = summaryOrNull(mainId, mainName, mainType, mainHeaviness)

private fun LunchRow.single(): DishSummary? = summaryOrNull(singleId, singleName, singleType, singleHeaviness)

private fun summaryOrNull(id: String?, name: String?, type: DishType?, heaviness: Heaviness?): DishSummary? {
    if (id == null || name == null) return null
    if (type == null || heaviness == null) return null
    return DishSummary(id = DishId(id), name = name, type = type, heaviness = heaviness)
}
