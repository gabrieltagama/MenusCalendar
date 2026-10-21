package com.gabrieltagama.menuplanner.core.domain.port

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId

/**
 * Persistence port of the recipe book aggregate. getDishes reads every dish once; upsertAll writes
 * several dishes in a single transaction; deleteIfUnused removes a dish only when it is not planned
 * in the calendar, atomically, and tells whether it was removed. The screens read the recipe book
 * through DishCatalogQueries and the planned days of a dish through PlannedUseQueries.
 * Implemented in :core:data.
 */
interface DishRepository {
    suspend fun getDishes(): List<Dish>
    suspend fun getDish(id: DishId): Dish?
    suspend fun upsert(dish: Dish)
    suspend fun upsertAll(dishes: List<Dish>)
    suspend fun deleteIfUnused(id: DishId): Boolean
}
