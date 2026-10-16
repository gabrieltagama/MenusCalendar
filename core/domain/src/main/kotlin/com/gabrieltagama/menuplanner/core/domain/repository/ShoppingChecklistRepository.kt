package com.gabrieltagama.menuplanner.core.domain.repository

import com.gabrieltagama.menuplanner.core.domain.model.ShoppingFilter
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Persistence port for the products ticked as bought in the shopping list, identified by their
 * IngredientKey value within the ShoppingFilter they were ticked in. clearEndedBefore drops the
 * ticks of filters whose range ended before a date. Implemented in :core:data.
 */
interface ShoppingChecklistRepository {
    fun checked(filter: ShoppingFilter): Flow<Set<String>>
    suspend fun setChecked(filter: ShoppingFilter, key: String, isChecked: Boolean)
    suspend fun clear(filter: ShoppingFilter)
    suspend fun clearEndedBefore(date: LocalDate)
}
