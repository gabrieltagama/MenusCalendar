package com.gabrieltagama.menuplanner.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.gabrieltagama.menuplanner.core.data.database.entity.DishEntity
import com.gabrieltagama.menuplanner.core.data.database.entity.DishVersion
import com.gabrieltagama.menuplanner.core.data.database.entity.DishWithIngredients
import com.gabrieltagama.menuplanner.core.data.database.entity.IngredientEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access for dishes and their ingredients. The dish row is upserted (never REPLACE) so
 * neither the ingredient cascade nor the calendar RESTRICT constraint fire on update; several dishes
 * can be written in one transaction with upsertAllWithIngredients.
 */
@Dao
internal interface DishDao {

    @Transaction
    @Query("SELECT * FROM dishes ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<DishWithIngredients>>

    @Transaction
    @Query("SELECT * FROM dishes WHERE type = :type ORDER BY name COLLATE NOCASE")
    fun observeByType(type: String): Flow<List<DishWithIngredients>>

    @Query("SELECT id, type, meal_time, updated_at FROM dishes")
    fun observeVersions(): Flow<List<DishVersion>>

    @Transaction
    @Query("SELECT * FROM dishes WHERE id = :id")
    suspend fun getById(id: String): DishWithIngredients?

    @Transaction
    @Query("SELECT * FROM dishes WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<DishWithIngredients>

    @Transaction
    @Query("SELECT * FROM dishes ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<DishWithIngredients>

    @Query("SELECT id, type, meal_time, updated_at FROM dishes")
    suspend fun getVersions(): List<DishVersion>

    @Query(
        "SELECT (SELECT COUNT(*) FROM meal_days WHERE starter_id = :id OR main_id = :id OR single_id = :id OR dessert_id = :id) + " +
            "(SELECT COUNT(*) FROM dinner_days WHERE dish_id = :id)"
    )
    suspend fun countUsages(id: String): Int

    @Upsert
    suspend fun upsertDish(dish: DishEntity)

    @Insert
    suspend fun insertIngredients(ingredients: List<IngredientEntity>)

    @Query("DELETE FROM ingredients WHERE dish_id = :dishId")
    suspend fun deleteIngredients(dishId: String)

    @Query("DELETE FROM dishes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Transaction
    suspend fun upsertWithIngredients(dish: DishEntity, ingredients: List<IngredientEntity>) {
        upsertDish(dish)
        deleteIngredients(dish.id)
        insertIngredients(ingredients)
    }

    @Transaction
    suspend fun upsertAllWithIngredients(dishes: List<Pair<DishEntity, List<IngredientEntity>>>) =
        dishes.forEach { (dish, ingredients) -> upsertWithIngredients(dish, ingredients) }
}
