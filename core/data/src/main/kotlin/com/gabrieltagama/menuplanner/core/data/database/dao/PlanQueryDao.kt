package com.gabrieltagama.menuplanner.core.data.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Read-only queries (CQRS query side) over both calendars. Each one joins the plan with the dishes
 * or ingredients it needs in a single statement, so Room observes every table involved and the flows
 * refresh when the plan or a planned dish changes, without loading whole dishes. A lunch row keeps
 * the same rule as the domain (MenuShape.of): either only single_id, or only starter_id and main_id;
 * a row mixing both shapes adds no ingredients.
 */
@Dao
internal interface PlanQueryDao {

    @Query(
        """
        SELECT m.date,
            s.id AS starter_id, s.name AS starter_name, s.type AS starter_type, s.heaviness AS starter_heaviness,
            mn.id AS main_id, mn.name AS main_name, mn.type AS main_type, mn.heaviness AS main_heaviness,
            sg.id AS single_id, sg.name AS single_name, sg.type AS single_type, sg.heaviness AS single_heaviness
        FROM meal_days m
        LEFT JOIN dishes s ON s.id = m.starter_id
        LEFT JOIN dishes mn ON mn.id = m.main_id
        LEFT JOIN dishes sg ON sg.id = m.single_id
        WHERE m.date BETWEEN :from AND :to
        ORDER BY m.date
        """
    )
    fun observeLunchRows(from: LocalDate, to: LocalDate): Flow<List<LunchRow>>

    @Query(
        """
        SELECT d.date, ds.id AS dish_id, ds.name AS dish_name, ds.type AS dish_type, ds.heaviness AS dish_heaviness
        FROM dinner_days d
        JOIN dishes ds ON ds.id = d.dish_id
        WHERE d.date BETWEEN :from AND :to
        ORDER BY d.date
        """
    )
    fun observeDinnerRows(from: LocalDate, to: LocalDate): Flow<List<DinnerRow>>

    @Query(
        """
        SELECT i.name, i.quantity, i.unit FROM (
            SELECT date, single_id AS dish_id, 2 AS slot FROM meal_days
                WHERE single_id IS NOT NULL AND starter_id IS NULL AND main_id IS NULL AND date BETWEEN :from AND :to
            UNION ALL SELECT date, starter_id, 0 FROM meal_days
                WHERE single_id IS NULL AND starter_id IS NOT NULL AND main_id IS NOT NULL AND date BETWEEN :from AND :to
            UNION ALL SELECT date, main_id, 1 FROM meal_days
                WHERE single_id IS NULL AND starter_id IS NOT NULL AND main_id IS NOT NULL AND date BETWEEN :from AND :to
        ) p
        JOIN ingredients i ON i.dish_id = p.dish_id
        ORDER BY p.date, p.slot, i.position
        """
    )
    fun observeLunchIngredientLines(from: LocalDate, to: LocalDate): Flow<List<IngredientLineRow>>

    @Query(
        """
        SELECT i.name, i.quantity, i.unit
        FROM dinner_days d
        JOIN ingredients i ON i.dish_id = d.dish_id
        WHERE d.date BETWEEN :from AND :to
        ORDER BY d.date, i.position
        """
    )
    fun observeDinnerIngredientLines(from: LocalDate, to: LocalDate): Flow<List<IngredientLineRow>>
}

internal data class LunchRow(
    val date: LocalDate,
    @ColumnInfo(name = "starter_id") val starterId: String?,
    @ColumnInfo(name = "starter_name") val starterName: String?,
    @ColumnInfo(name = "starter_type") val starterType: DishType?,
    @ColumnInfo(name = "starter_heaviness") val starterHeaviness: Heaviness?,
    @ColumnInfo(name = "main_id") val mainId: String?,
    @ColumnInfo(name = "main_name") val mainName: String?,
    @ColumnInfo(name = "main_type") val mainType: DishType?,
    @ColumnInfo(name = "main_heaviness") val mainHeaviness: Heaviness?,
    @ColumnInfo(name = "single_id") val singleId: String?,
    @ColumnInfo(name = "single_name") val singleName: String?,
    @ColumnInfo(name = "single_type") val singleType: DishType?,
    @ColumnInfo(name = "single_heaviness") val singleHeaviness: Heaviness?
)

internal data class DinnerRow(
    val date: LocalDate,
    @ColumnInfo(name = "dish_id") val dishId: String,
    @ColumnInfo(name = "dish_name") val dishName: String,
    @ColumnInfo(name = "dish_type") val dishType: DishType,
    @ColumnInfo(name = "dish_heaviness") val dishHeaviness: Heaviness
)

internal data class IngredientLineRow(
    val name: String,
    val quantity: Double,
    val unit: MeasureUnit
)
