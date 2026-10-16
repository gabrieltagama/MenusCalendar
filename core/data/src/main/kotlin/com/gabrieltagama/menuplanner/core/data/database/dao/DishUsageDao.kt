package com.gabrieltagama.menuplanner.core.data.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Read-only queries over both calendars: the last epoch day up to a limit (included) each dish
 * was planned, as starter, main or single dish at lunch, or at dinner.
 */
@Dao
internal interface DishUsageDao {

    @Query(
        """
        SELECT dish_id, MAX(date) AS last_date FROM (
            SELECT starter_id AS dish_id, date FROM meal_days WHERE starter_id IS NOT NULL AND date <= :untilEpochDay
            UNION ALL SELECT main_id, date FROM meal_days WHERE main_id IS NOT NULL AND date <= :untilEpochDay
            UNION ALL SELECT single_id, date FROM meal_days WHERE single_id IS NOT NULL AND date <= :untilEpochDay
            UNION ALL SELECT dish_id, date FROM dinner_days WHERE date <= :untilEpochDay
        ) GROUP BY dish_id
        """
    )
    fun observeLastServed(untilEpochDay: Long): Flow<List<DishLastServed>>
}

internal data class DishLastServed(
    @ColumnInfo(name = "dish_id") val dishId: String,
    @ColumnInfo(name = "last_date") val lastDate: Long
)
