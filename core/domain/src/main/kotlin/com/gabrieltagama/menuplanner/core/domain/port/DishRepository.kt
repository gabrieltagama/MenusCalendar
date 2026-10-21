package com.gabrieltagama.menuplanner.core.domain.port

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.PlannedUse
import kotlinx.coroutines.flow.Flow

/**
 * Persistence port for the recipe book. getDishes reads every dish once; upsertAll writes several
 * dishes in a single transaction; deleteIfUnused removes a dish only when it is not planned in the
 * calendar, atomically, and tells whether it was removed; getPlannedUses lists the calendar days
 * where a dish is planned. Implemented in :core:data.
 */
interface DishRepository {
    fun observeDishes(): Flow<List<Dish>>
    fun observeDishesByType(type: DishType): Flow<List<Dish>>
    suspend fun getDishes(): List<Dish>
    suspend fun getDish(id: DishId): Dish?
    suspend fun upsert(dish: Dish)
    suspend fun upsertAll(dishes: List<Dish>)
    suspend fun deleteIfUnused(id: DishId): Boolean
    suspend fun getPlannedUses(id: DishId): List<PlannedUse>
}
