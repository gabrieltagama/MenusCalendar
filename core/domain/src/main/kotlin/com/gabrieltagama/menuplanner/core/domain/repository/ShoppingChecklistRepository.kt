package com.gabrieltagama.menuplanner.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Persistence port for the products ticked as bought in the shopping list, identified by their
 * IngredientKey value. Implemented in :core:data.
 */
interface ShoppingChecklistRepository {
    val checked: Flow<Set<String>>
    suspend fun setChecked(key: String, isChecked: Boolean)
    suspend fun clear()
}
