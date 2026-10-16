package com.gabrieltagama.menuplanner.core.domain.model.plan

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness

/**
 * Read model of a planned dish: only what the calendar shows (name, type and heaviness), without
 * ingredients or preparation. It is a query result, never saved back; commands use Dish.
 */
data class DishSummary(
    val id: DishId,
    val name: String,
    val type: DishType,
    val heaviness: Heaviness
)

fun Dish.toSummary(): DishSummary = DishSummary(id = id, name = name, type = type, heaviness = heaviness)
