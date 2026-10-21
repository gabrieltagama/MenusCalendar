package com.gabrieltagama.menuplanner.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.gabrieltagama.menuplanner.core.data.database.entity.LunchDayEntity
import java.time.LocalDate

/**
 * Data access for planned days, addressed by date.
 */
@Dao
internal interface LunchDayDao : DayPlanDao<LunchDayEntity> {

    @Query("SELECT * FROM meal_days WHERE date BETWEEN :from AND :to ORDER BY date")
    override suspend fun getRange(from: LocalDate, to: LocalDate): List<LunchDayEntity>

    @Query("SELECT * FROM meal_days WHERE date = :date")
    override suspend fun getByDate(date: LocalDate): LunchDayEntity?

    @Query("SELECT * FROM meal_days ORDER BY date")
    override suspend fun getAll(): List<LunchDayEntity>

    @Upsert
    override suspend fun upsert(day: LunchDayEntity)

    @Upsert
    override suspend fun upsertAll(days: List<LunchDayEntity>)

    @Query("DELETE FROM meal_days WHERE date = :date")
    override suspend fun deleteByDate(date: LocalDate)

    @Query("DELETE FROM meal_days WHERE date IN (:dates)")
    override suspend fun deleteByDates(dates: List<LocalDate>)
}
