package com.gabrieltagama.menuplanner.core.data.repository

import com.gabrieltagama.menuplanner.core.data.database.dao.DishDao
import com.gabrieltagama.menuplanner.core.data.database.dao.MealDayDao
import com.gabrieltagama.menuplanner.core.data.database.entity.MealDayEntity
import com.gabrieltagama.menuplanner.core.data.database.entity.dishIds
import com.gabrieltagama.menuplanner.core.data.mapper.toDomain
import com.gabrieltagama.menuplanner.core.data.mapper.toEntity
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.repository.MealPlanRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Room-backed lunch calendar over the meal_days table (see RoomPlanRepository).
 */
internal class RoomMealPlanRepository @Inject constructor(
    private val mealDayDao: MealDayDao,
    dishDao: DishDao
) : RoomPlanRepository<MealDayEntity, MealDay>(dishDao), MealPlanRepository {

    override fun observeRows(fromEpochDay: Long, toEpochDay: Long): Flow<List<MealDayEntity>> = mealDayDao.observeRange(fromEpochDay, toEpochDay)

    override suspend fun rows(fromEpochDay: Long, toEpochDay: Long): List<MealDayEntity> = mealDayDao.getRange(fromEpochDay, toEpochDay)

    override suspend fun row(epochDay: Long): MealDayEntity? = mealDayDao.getByDate(epochDay)

    override suspend fun upsertRows(rows: List<MealDayEntity>) = mealDayDao.upsertAll(rows)

    override suspend fun deleteRow(epochDay: Long) = mealDayDao.deleteByDate(epochDay)

    override fun dishIdsOf(row: MealDayEntity): List<String> = row.dishIds

    override fun toDomain(row: MealDayEntity, dishesById: Map<String, Dish>): MealDay? = row.toDomain(dishesById)

    override fun toRow(day: MealDay): MealDayEntity = day.toEntity()
}
