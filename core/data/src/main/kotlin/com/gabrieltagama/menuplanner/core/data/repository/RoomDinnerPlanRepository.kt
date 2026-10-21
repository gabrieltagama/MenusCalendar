package com.gabrieltagama.menuplanner.core.data.repository

import com.gabrieltagama.menuplanner.core.data.database.dao.DinnerDayDao
import com.gabrieltagama.menuplanner.core.data.database.dao.DishDao
import com.gabrieltagama.menuplanner.core.data.database.entity.DinnerDayEntity
import com.gabrieltagama.menuplanner.core.data.mapper.toDomain
import com.gabrieltagama.menuplanner.core.data.mapper.toEntity
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.port.DinnerPlanRepository
import javax.inject.Inject

/**
 * Room-backed dinner calendar (see RoomDayPlanRepository): a dinner row references a single dish.
 */
internal class RoomDinnerPlanRepository @Inject constructor(
    dinnerDayDao: DinnerDayDao,
    dishDao: DishDao
) : RoomDayPlanRepository<DinnerDayEntity, DinnerDay>(
    dao = dinnerDayDao,
    dishDao = dishDao,
    dishIdsOf = { listOf(it.dishId) },
    toDomain = DinnerDayEntity::toDomain,
    toEntity = DinnerDay::toEntity
), DinnerPlanRepository
