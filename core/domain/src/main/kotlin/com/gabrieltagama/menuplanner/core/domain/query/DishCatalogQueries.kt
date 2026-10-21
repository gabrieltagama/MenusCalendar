package com.gabrieltagama.menuplanner.core.domain.query

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import kotlinx.coroutines.flow.Flow

/**
 * Read port of the recipe book for the screens: every dish, emitted again whenever one changes.
 * Implemented in :core:data.
 */
interface DishCatalogQueries {
    fun observeDishes(): Flow<List<Dish>>
}
