package com.gabrieltagama.menuplanner.core.data.repository

import com.gabrieltagama.menuplanner.core.data.database.dao.DishDao
import com.gabrieltagama.menuplanner.core.data.database.dao.MealDayDao
import com.gabrieltagama.menuplanner.core.data.database.entity.MealDayEntity
import com.gabrieltagama.menuplanner.core.data.database.entity.dishIds
import com.gabrieltagama.menuplanner.core.data.mapper.toDomain
import com.gabrieltagama.menuplanner.core.data.mapper.toEntity
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.port.MealPlanRepository
import javax.inject.Inject

/**
 * Room-backed lunch calendar (see RoomDayPlanRepository): a lunch row references up to three dishes.
 */
internal class RoomMealPlanRepository @Inject constructor(
    mealDayDao: MealDayDao,
    dishDao: DishDao
) : RoomDayPlanRepository<MealDayEntity, MealDay>(
    dao = mealDayDao,
    dishDao = dishDao,
    dishIdsOf = MealDayEntity::dishIds,
    toDomain = MealDayEntity::toDomain,
    toEntity = MealDay::toEntity
), MealPlanRepository
