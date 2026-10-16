package com.gabrieltagama.menuplanner.core.data.mapper

import com.gabrieltagama.menuplanner.core.data.database.entity.DinnerDayEntity
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import java.time.Instant
import java.time.LocalDate

/**
 * Maps planned dinners between Room rows and the domain; a row whose dish is unknown maps to null.
 */
internal fun DinnerDayEntity.toDomain(dishesById: Map<DishId, Dish>): DinnerDay? =
    dishesById[DishId(dishId)]?.let { dish ->
        DinnerDay(date = LocalDate.ofEpochDay(date), dish = dish, updatedAt = Instant.ofEpochMilli(updatedAt))
    }

internal fun DinnerDay.toEntity(): DinnerDayEntity =
    DinnerDayEntity(date = date.toEpochDay(), dishId = dish.id.value, updatedAt = updatedAt.toEpochMilli())
