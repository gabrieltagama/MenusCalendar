package com.gabrieltagama.menuplanner.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.gabrieltagama.menuplanner.core.data.database.entity.LunchDayEntity

/**
 * Data access for planned days, addressed by epoch day.
 */
@Dao
internal interface LunchDayDao : DayPlanDao<LunchDayEntity> {

    @Query("SELECT * FROM meal_days WHERE date BETWEEN :fromEpochDay AND :toEpochDay ORDER BY date")
    override suspend fun getRange(fromEpochDay: Long, toEpochDay: Long): List<LunchDayEntity>

    @Query("SELECT * FROM meal_days WHERE date = :epochDay")
    override suspend fun getByDate(epochDay: Long): LunchDayEntity?

    @Query("SELECT * FROM meal_days ORDER BY date")
    override suspend fun getAll(): List<LunchDayEntity>

    @Upsert
    override suspend fun upsert(day: LunchDayEntity)

    @Upsert
    override suspend fun upsertAll(days: List<LunchDayEntity>)

    @Query("DELETE FROM meal_days WHERE date = :epochDay")
    override suspend fun deleteByDate(epochDay: Long)

    @Query("DELETE FROM meal_days WHERE date IN (:epochDays)")
    override suspend fun deleteByDates(epochDays: List<Long>)
}
