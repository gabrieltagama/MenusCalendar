package com.gabrieltagama.menuplanner.core.data.mapper

import com.gabrieltagama.menuplanner.core.data.database.entity.MealDayEntity
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import java.time.Instant
import java.time.LocalDate

/**
 * Maps planned days between Room rows and the domain. A row with single_id becomes a Single
 * menu, otherwise a Courses menu; a day with any unresolved dish reference maps to null.
 */
internal fun MealDayEntity.toDomain(dishesById: Map<DishId, Dish>): MealDay? =
    toMenu(dishesById)?.let { menu ->
        MealDay(date = LocalDate.ofEpochDay(date), menu = menu, updatedAt = Instant.ofEpochMilli(updatedAt))
    }

internal fun MealDay.toEntity(): MealDayEntity = MealDayEntity(
    date = date.toEpochDay(),
    starterId = (menu as? DailyMenu.Courses)?.starter?.id?.value,
    mainId = (menu as? DailyMenu.Courses)?.main?.id?.value,
    singleId = (menu as? DailyMenu.Single)?.single?.id?.value,
    updatedAt = updatedAt.toEpochMilli()
)

private fun MealDayEntity.toMenu(dishesById: Map<DishId, Dish>): DailyMenu? {
    if (singleId != null) return dishesById[DishId(singleId)]?.let { DailyMenu.Single(single = it) }
    val starter = starterId?.let { dishesById[DishId(it)] } ?: return null
    val main = mainId?.let { dishesById[DishId(it)] } ?: return null
    return DailyMenu.Courses(starter = starter, main = main)
}
