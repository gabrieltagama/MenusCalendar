package com.gabrieltagama.menuplanner.core.data.mapper

import com.gabrieltagama.menuplanner.core.data.database.entity.LunchDayEntity
import com.gabrieltagama.menuplanner.core.data.database.entity.slotIds
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.LunchDay

/**
 * Maps planned days between Room rows and the domain. The filled dish columns must be exactly the
 * slots of one menu shape (DailyMenu.fromSlots), so a row mixing a single dish with a starter or a
 * main, or with any unresolved dish reference, maps to null.
 */
internal fun LunchDayEntity.toDomain(dishesById: Map<DishId, Dish>): LunchDay? =
    toMenu(dishesById)?.let { menu ->
        LunchDay(date = date, menu = menu, updatedAt = updatedAt)
    }

internal fun LunchDay.toEntity(): LunchDayEntity = LunchDayEntity(
    date = date,
    starterId = (menu as? DailyMenu.Courses)?.starter?.id?.value,
    mainId = (menu as? DailyMenu.Courses)?.main?.id?.value,
    singleId = (menu as? DailyMenu.Single)?.single?.id?.value,
    updatedAt = updatedAt
)

private fun LunchDayEntity.toMenu(dishesById: Map<DishId, Dish>): DailyMenu? =
    DailyMenu.fromSlots(slotIds.mapValues { (_, id) -> dishesById[DishId(id)] ?: return null })
