package com.gabrieltagama.menuplanner.core.domain.query

import com.gabrieltagama.menuplanner.core.domain.model.ShoppingFilter
import com.gabrieltagama.menuplanner.core.domain.model.measure.IngredientKey
import kotlinx.coroutines.flow.Flow

/**
 * Read port of the products ticked as bought in the shopping list of one ShoppingFilter, by their
 * IngredientKey. ShoppingChecklistRepository adds the changes. Implemented in :core:data.
 */
interface ShoppingChecklistQueries {
    fun checked(filter: ShoppingFilter): Flow<Set<IngredientKey>>
}
