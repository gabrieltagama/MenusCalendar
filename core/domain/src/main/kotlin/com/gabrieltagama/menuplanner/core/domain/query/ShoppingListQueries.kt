package com.gabrieltagama.menuplanner.core.domain.query

import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Read port (CQRS query side) of the shopping list: the ingredient lines of every dish planned for
 * one meal between two dates (both included), once per planned dish, ordered by date, menu slot and
 * ingredient position. The flow emits again whenever the plan or a planned dish changes.
 * Implemented in :core:data.
 */
interface ShoppingListQueries {
    fun observeIngredientLines(dates: ClosedRange<LocalDate>, meal: Meal): Flow<List<Ingredient>>
}
