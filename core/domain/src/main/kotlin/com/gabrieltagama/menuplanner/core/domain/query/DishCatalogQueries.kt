package com.gabrieltagama.menuplanner.core.domain.query

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishListItem
import kotlinx.coroutines.flow.Flow

/**
 * Read port of the recipe book for the screens, emitted again whenever a dish changes:
 * observeDishList gives the DishListItem read model the recipe book list needs, and observeDishes the
 * whole dishes the lunch and dinner editors plan with. Implemented in :core:data.
 */
interface DishCatalogQueries {
    fun observeDishList(): Flow<List<DishListItem>>
    fun observeDishes(): Flow<List<Dish>>
}
