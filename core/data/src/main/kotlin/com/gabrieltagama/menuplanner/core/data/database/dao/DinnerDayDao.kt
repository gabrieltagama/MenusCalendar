package com.gabrieltagama.menuplanner.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.gabrieltagama.menuplanner.core.data.database.entity.DinnerDayEntity

/**
 * Data access for planned dinners, addressed by epoch day.
 */
@Dao
internal interface DinnerDayDao : DayPlanDao<DinnerDayEntity> {

    @Query("SELECT * FROM dinner_days WHERE date BETWEEN :fromEpochDay AND :toEpochDay ORDER BY date")
    override suspend fun getRange(fromEpochDay: Long, toEpochDay: Long): List<DinnerDayEntity>

    @Query("SELECT * FROM dinner_days WHERE date = :epochDay")
    override suspend fun getByDate(epochDay: Long): DinnerDayEntity?

    @Query("SELECT * FROM dinner_days ORDER BY date")
    override suspend fun getAll(): List<DinnerDayEntity>

    @Upsert
    override suspend fun upsert(day: DinnerDayEntity)

    @Upsert
    override suspend fun upsertAll(days: List<DinnerDayEntity>)

    @Query("DELETE FROM dinner_days WHERE date = :epochDay")
    override suspend fun deleteByDate(epochDay: Long)

    @Query("DELETE FROM dinner_days WHERE date IN (:epochDays)")
    override suspend fun deleteByDates(epochDays: List<Long>)
}
