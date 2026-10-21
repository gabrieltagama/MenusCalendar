package com.gabrieltagama.menuplanner.core.data.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Read-only queries over both calendars: the last day up to a limit (included) each dish
 * was planned, as starter, main or single dish at lunch, or at dinner. Lunch rows follow the menu
 * shapes of the domain (only single_id, or only starter_id and main_id); a mixed row counts for none.
 */
@Dao
internal interface DishUsageDao {

    @Query(
        """
        SELECT dish_id, MAX(date) AS last_date FROM (
            SELECT starter_id AS dish_id, date FROM meal_days
                WHERE single_id IS NULL AND starter_id IS NOT NULL AND main_id IS NOT NULL AND date <= :until
            UNION ALL SELECT main_id, date FROM meal_days
                WHERE single_id IS NULL AND starter_id IS NOT NULL AND main_id IS NOT NULL AND date <= :until
            UNION ALL SELECT single_id, date FROM meal_days
                WHERE single_id IS NOT NULL AND starter_id IS NULL AND main_id IS NULL AND date <= :until
            UNION ALL SELECT dish_id, date FROM dinner_days WHERE date <= :until
        ) GROUP BY dish_id
        """
    )
    fun observeLastServed(until: LocalDate): Flow<List<DishLastServed>>
}

internal data class DishLastServed(
    @ColumnInfo(name = "dish_id") val dishId: String,
    @ColumnInfo(name = "last_date") val lastDate: LocalDate
)
