package com.gabrieltagama.menuplanner.core.data.repository

import com.gabrieltagama.menuplanner.core.data.database.dao.DinnerDayDao
import com.gabrieltagama.menuplanner.core.data.database.dao.DishDao
import com.gabrieltagama.menuplanner.core.data.database.entity.DinnerDayEntity
import com.gabrieltagama.menuplanner.core.data.mapper.toDomain
import com.gabrieltagama.menuplanner.core.data.mapper.toEntity
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.port.DinnerPlanRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Room-backed dinner calendar over the dinner_days table (see RoomPlanRepository).
 */
internal class RoomDinnerPlanRepository @Inject constructor(
    private val dinnerDayDao: DinnerDayDao,
    dishDao: DishDao
) : RoomPlanRepository<DinnerDayEntity, DinnerDay>(dishDao), DinnerPlanRepository {

    override fun observeRows(fromEpochDay: Long, toEpochDay: Long): Flow<List<DinnerDayEntity>> = dinnerDayDao.observeRange(fromEpochDay, toEpochDay)

    override suspend fun rows(fromEpochDay: Long, toEpochDay: Long): List<DinnerDayEntity> = dinnerDayDao.getRange(fromEpochDay, toEpochDay)

    override suspend fun row(epochDay: Long): DinnerDayEntity? = dinnerDayDao.getByDate(epochDay)

    override suspend fun upsertRows(rows: List<DinnerDayEntity>) = dinnerDayDao.upsertAll(rows)

    override suspend fun deleteRow(epochDay: Long) = dinnerDayDao.deleteByDate(epochDay)

    override fun dishIdsOf(row: DinnerDayEntity): List<String> = listOf(row.dishId)

    override fun toDomain(row: DinnerDayEntity, dishesById: Map<String, Dish>): DinnerDay? = row.toDomain(dishesById)

    override fun toRow(day: DinnerDay): DinnerDayEntity = day.toEntity()
}
