package com.gabrieltagama.menuplanner.core.data.mapper

import com.gabrieltagama.menuplanner.core.data.database.entity.DinnerDayEntity
import com.gabrieltagama.menuplanner.core.data.database.entity.LunchDayEntity
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.share.DinnerEntry
import com.gabrieltagama.menuplanner.core.domain.model.share.LunchEntry
import java.time.Instant
import java.time.LocalDate

/**
 * Maps planned days between Room rows and the dish references of a snapshot.
 */
internal fun LunchDayEntity.toEntry(): LunchEntry = LunchEntry(
    date = LocalDate.ofEpochDay(date),
    starterId = starterId?.let(::DishId),
    mainId = mainId?.let(::DishId),
    singleId = singleId?.let(::DishId),
    updatedAt = Instant.ofEpochMilli(updatedAt)
)

internal fun DinnerDayEntity.toEntry(): DinnerEntry =
    DinnerEntry(date = LocalDate.ofEpochDay(date), dishId = DishId(dishId), updatedAt = Instant.ofEpochMilli(updatedAt))
