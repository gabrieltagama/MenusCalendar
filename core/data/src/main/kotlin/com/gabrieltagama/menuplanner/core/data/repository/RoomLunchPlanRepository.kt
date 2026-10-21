package com.gabrieltagama.menuplanner.core.data.repository

import com.gabrieltagama.menuplanner.core.data.database.dao.DishDao
import com.gabrieltagama.menuplanner.core.data.database.dao.LunchDayDao
import com.gabrieltagama.menuplanner.core.data.database.entity.LunchDayEntity
import com.gabrieltagama.menuplanner.core.data.database.entity.dishIds
import com.gabrieltagama.menuplanner.core.data.mapper.toDomain
import com.gabrieltagama.menuplanner.core.data.mapper.toEntity
import com.gabrieltagama.menuplanner.core.domain.model.LunchDay
import com.gabrieltagama.menuplanner.core.domain.port.LunchPlanRepository
import javax.inject.Inject

/**
 * Room-backed lunch calendar (see RoomDayPlanRepository): a lunch row references up to three dishes.
 */
internal class RoomLunchPlanRepository @Inject constructor(
    lunchDayDao: LunchDayDao,
    dishDao: DishDao
) : RoomDayPlanRepository<LunchDayEntity, LunchDay>(
        dao = lunchDayDao,
        dishDao = dishDao,
        dishIdsOf = LunchDayEntity::dishIds,
        toDomain = LunchDayEntity::toDomain,
        toEntity = LunchDay::toEntity
    ),
    LunchPlanRepository
