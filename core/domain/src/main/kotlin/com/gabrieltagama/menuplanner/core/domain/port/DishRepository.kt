package com.gabrieltagama.menuplanner.core.domain.port

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import kotlinx.coroutines.flow.Flow

/**
 * Persistence port for the recipe book. getDishes is a one-shot read of every dish, safe to call
 * inside a TransactionRunner block. upsertAll stores many dishes at once and atomically in
 * real implementations; the default saving them one by one is only meant for simple fakes.
 * Implemented in :core:data.
 */
interface DishRepository {
    fun observeDishes(): Flow<List<Dish>>
    fun observeDishesByType(type: DishType): Flow<List<Dish>>
    suspend fun getDishes(): List<Dish>
    suspend fun getDish(id: String): Dish?
    suspend fun upsert(dish: Dish)
    suspend fun upsertAll(dishes: List<Dish>) = dishes.forEach { upsert(it) }
    suspend fun delete(id: String)
    suspend fun isUsedInCalendar(id: String): Boolean
}
