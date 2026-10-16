package com.gabrieltagama.menuplanner.core.data.mapper

import com.gabrieltagama.menuplanner.core.data.database.dao.DinnerRow
import com.gabrieltagama.menuplanner.core.data.database.dao.IngredientLineRow
import com.gabrieltagama.menuplanner.core.data.database.dao.LunchRow
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.MenuSlot
import com.gabrieltagama.menuplanner.core.domain.model.plan.DishSummary
import com.gabrieltagama.menuplanner.core.domain.model.plan.PlannedDinner
import com.gabrieltagama.menuplanner.core.domain.model.plan.PlannedLunch
import java.time.LocalDate

/**
 * Maps the rows of PlanQueryDao to read models. Like MealDayMapper, a lunch with single_id is a
 * single dish, otherwise it needs both starter and main; an incomplete row maps to null.
 */
internal fun LunchRow.toPlannedLunch(): PlannedLunch? = toSlots()?.let { PlannedLunch(date = LocalDate.ofEpochDay(date), slots = it) }

internal fun DinnerRow.toPlannedDinner(): PlannedDinner =
    PlannedDinner(date = LocalDate.ofEpochDay(date), dish = summaryOf(dishId, dishName, dishType, dishHeaviness))

internal fun IngredientLineRow.toDomain(): Ingredient = Ingredient(name = name, quantity = quantity, unit = MeasureUnit.valueOf(unit))

private fun LunchRow.toSlots(): List<Pair<MenuSlot, DishSummary>>? {
    if (singleId != null) return single()?.let { listOf(MenuSlot.SINGLE to it) }
    val starter = starter() ?: return null
    val main = main() ?: return null
    return listOf(MenuSlot.STARTER to starter, MenuSlot.MAIN to main)
}

private fun LunchRow.starter(): DishSummary? = summaryOrNull(starterId, starterName, starterType, starterHeaviness)

private fun LunchRow.main(): DishSummary? = summaryOrNull(mainId, mainName, mainType, mainHeaviness)

private fun LunchRow.single(): DishSummary? = summaryOrNull(singleId, singleName, singleType, singleHeaviness)

private fun summaryOrNull(id: String?, name: String?, type: String?, heaviness: String?): DishSummary? =
    if (id == null || name == null || type == null || heaviness == null) null else summaryOf(id, name, type, heaviness)

private fun summaryOf(id: String, name: String, type: String, heaviness: String): DishSummary =
    DishSummary(id = DishId(id), name = name, type = DishType.valueOf(type), heaviness = Heaviness.valueOf(heaviness))
