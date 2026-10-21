package com.gabrieltagama.menuplanner.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.gabrieltagama.menuplanner.core.data.database.entity.DinnerDayEntity
import java.time.LocalDate

/**
 * Data access for planned dinners, addressed by date.
 */
@Dao
internal interface DinnerDayDao : DayPlanDao<DinnerDayEntity> {

    @Query("SELECT * FROM dinner_days WHERE date BETWEEN :from AND :to ORDER BY date")
    override suspend fun getRange(from: LocalDate, to: LocalDate): List<DinnerDayEntity>

    @Query("SELECT * FROM dinner_days WHERE date = :date")
    override suspend fun getByDate(date: LocalDate): DinnerDayEntity?

    @Query("SELECT * FROM dinner_days ORDER BY date")
    override suspend fun getAll(): List<DinnerDayEntity>

    @Upsert
    override suspend fun upsert(day: DinnerDayEntity)

    @Upsert
    override suspend fun upsertAll(days: List<DinnerDayEntity>)

    @Query("DELETE FROM dinner_days WHERE date = :date")
    override suspend fun deleteByDate(date: LocalDate)

    @Query("DELETE FROM dinner_days WHERE date IN (:dates)")
    override suspend fun deleteByDates(dates: List<LocalDate>)
}
