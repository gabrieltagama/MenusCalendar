package com.gabrieltagama.menuplanner.feature.dishes.list

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.model.Rating

/**
 * Pure filtering of the recipe book table. Every filter is a set of accepted values (multiple
 * selection) and an empty set means "no filter". A dish is kept when its type, heaviness, rating
 * and meal time are each in their set and when the case- and accent-insensitive query is
 * contained in its name or in the name of any of its ingredients. Results are sorted
 * alphabetically ignoring accents and case.
 */
data class DishFilterCriteria(
    val query: String = "",
    val types: Set<DishType> = emptySet(),
    val heaviness: Set<Heaviness> = emptySet(),
    val ratings: Set<Rating> = emptySet(),
    val mealTimes: Set<MealTime> = emptySet()
)

internal object DishFilter {

    fun apply(dishes: List<Dish>, criteria: DishFilterCriteria): List<Dish> {
        val normalizedQuery = criteria.query.normalizedForSearch()
        return dishes
            .filter { criteria.types.accepts(it.type) }
            .filter { criteria.heaviness.accepts(it.heaviness) }
            .filter { criteria.ratings.accepts(it.rating) }
            .filter { criteria.mealTimes.accepts(it.mealTime) }
            .filter { it.matches(normalizedQuery) }
            .sortedBy { it.name.normalizedForSearch() }
    }

    private fun <T> Set<T>.accepts(value: T): Boolean = isEmpty() || value in this

    private fun Dish.matches(normalizedQuery: String): Boolean =
        normalizedQuery.isEmpty() ||
            name.containsNormalized(normalizedQuery) ||
            ingredients.any { it.name.containsNormalized(normalizedQuery) }
}
