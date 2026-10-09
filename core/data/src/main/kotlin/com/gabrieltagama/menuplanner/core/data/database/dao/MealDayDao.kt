package com.gabrieltagama.menuplanner.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.gabrieltagama.menuplanner.core.data.database.entity.MealDayEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access for planned days, addressed by epoch day.
 */
@Dao
internal interface MealDayDao {

    @Query("SELECT * FROM meal_days WHERE date BETWEEN :fromEpochDay AND :toEpochDay ORDER BY date")
    fun observeRange(fromEpochDay: Long, toEpochDay: Long): Flow<List<MealDayEntity>>

    @Query("SELECT * FROM meal_days WHERE date = :epochDay")
    suspend fun getByDate(epochDay: Long): MealDayEntity?

    @Query("SELECT * FROM meal_days ORDER BY date")
    suspend fun getAll(): List<MealDayEntity>

    @Upsert
    suspend fun upsert(day: MealDayEntity)

    @Query("DELETE FROM meal_days WHERE date = :epochDay")
    suspend fun deleteByDate(epochDay: Long)
}
