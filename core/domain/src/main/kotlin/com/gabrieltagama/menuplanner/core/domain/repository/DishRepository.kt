package com.gabrieltagama.menuplanner.core.domain.repository

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import kotlinx.coroutines.flow.Flow

/**
 * Persistence port for the recipe book. Implemented in :core:data.
 */
interface DishRepository {
    fun observeDishes(): Flow<List<Dish>>
    fun observeDishesByType(type: DishType): Flow<List<Dish>>
    suspend fun getDish(id: String): Dish?
    suspend fun upsert(dish: Dish)
    suspend fun delete(id: String)
    suspend fun isUsedInCalendar(id: String): Boolean
}
