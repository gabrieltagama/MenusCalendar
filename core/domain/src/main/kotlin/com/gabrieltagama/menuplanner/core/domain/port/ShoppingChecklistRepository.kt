package com.gabrieltagama.menuplanner.core.domain.port

import com.gabrieltagama.menuplanner.core.domain.model.ShoppingFilter
import com.gabrieltagama.menuplanner.core.domain.model.measure.IngredientKey
import com.gabrieltagama.menuplanner.core.domain.query.ShoppingChecklistQueries
import java.time.LocalDate

/**
 * Persistence port for the products ticked as bought in the shopping list, identified by their
 * IngredientKey within the ShoppingFilter they were ticked in (read through
 * ShoppingChecklistQueries). clearEndedBefore drops the ticks of filters whose range ended before a
 * date. Implemented in :core:data.
 */
interface ShoppingChecklistRepository : ShoppingChecklistQueries {
    suspend fun setChecked(filter: ShoppingFilter, key: IngredientKey, isChecked: Boolean)
    suspend fun clear(filter: ShoppingFilter)
    suspend fun clearEndedBefore(date: LocalDate)
}
