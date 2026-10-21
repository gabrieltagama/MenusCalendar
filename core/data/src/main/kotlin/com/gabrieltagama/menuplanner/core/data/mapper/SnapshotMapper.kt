package com.gabrieltagama.menuplanner.core.data.mapper

import com.gabrieltagama.menuplanner.core.data.database.entity.DinnerDayEntity
import com.gabrieltagama.menuplanner.core.data.database.entity.LunchDayEntity
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.share.DinnerEntry
import com.gabrieltagama.menuplanner.core.domain.model.share.LunchEntry

/**
 * Maps planned days between Room rows and the dish references of a snapshot.
 */
internal fun LunchDayEntity.toEntry(): LunchEntry = LunchEntry(
    date = date,
    starterId = starterId?.let(::DishId),
    mainId = mainId?.let(::DishId),
    singleId = singleId?.let(::DishId),
    updatedAt = updatedAt
)

internal fun DinnerDayEntity.toEntry(): DinnerEntry =
    DinnerEntry(date = date, dishId = DishId(dishId), updatedAt = updatedAt)
