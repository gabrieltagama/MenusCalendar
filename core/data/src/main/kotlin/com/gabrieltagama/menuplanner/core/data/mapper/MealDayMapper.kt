package com.gabrieltagama.menuplanner.core.data.mapper

import com.gabrieltagama.menuplanner.core.data.database.entity.MealDayEntity
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import java.time.Instant
import java.time.LocalDate

/**
 * Maps planned days between Room rows and the domain. A row with single_id becomes a Single
 * menu, otherwise a Courses menu; a day with any unresolved dish reference maps to null.
 */
internal fun MealDayEntity.toDomain(dishesById: Map<String, Dish>): MealDay? =
    toMenu(dishesById)?.let { menu ->
        MealDay(date = LocalDate.ofEpochDay(date), menu = menu, updatedAt = Instant.ofEpochMilli(updatedAt))
    }

internal fun MealDay.toEntity(): MealDayEntity = MealDayEntity(
    date = date.toEpochDay(),
    starterId = (menu as? DailyMenu.Courses)?.starter?.id,
    mainId = (menu as? DailyMenu.Courses)?.main?.id,
    singleId = (menu as? DailyMenu.Single)?.single?.id,
    dessertId = menu.dessert?.id,
    updatedAt = updatedAt.toEpochMilli()
)

private fun MealDayEntity.toMenu(dishesById: Map<String, Dish>): DailyMenu? {
    val dessert = dessertId?.let { dishesById[it] ?: return null }
    if (singleId != null) return dishesById[singleId]?.let { DailyMenu.Single(single = it, dessert = dessert) }
    val starter = starterId?.let(dishesById::get) ?: return null
    val main = mainId?.let(dishesById::get) ?: return null
    return DailyMenu.Courses(starter = starter, main = main, dessert = dessert)
}
