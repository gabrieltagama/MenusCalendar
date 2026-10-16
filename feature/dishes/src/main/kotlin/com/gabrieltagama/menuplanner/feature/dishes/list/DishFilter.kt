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
 * alphabetically ignoring accents and case. The normalized texts are computed once per recipe
 * book version (index), not on every keystroke.
 */
data class DishFilterCriteria(
    val query: String = "",
    val types: Set<DishType> = emptySet(),
    val heaviness: Set<Heaviness> = emptySet(),
    val ratings: Set<Rating> = emptySet(),
    val mealTimes: Set<MealTime> = emptySet()
)

internal class SearchableDish(val dish: Dish) {
    val sortKey: String = dish.name.normalizedForSearch()
    private val searchableTexts: List<String> = listOf(sortKey) + dish.ingredients.map { it.name.normalizedForSearch() }

    fun matches(normalizedQuery: String): Boolean = normalizedQuery.isEmpty() || searchableTexts.any { normalizedQuery in it }
}

internal object DishFilter {

    fun index(dishes: List<Dish>): List<SearchableDish> = dishes.map(::SearchableDish).sortedBy(SearchableDish::sortKey)

    fun apply(dishes: List<Dish>, criteria: DishFilterCriteria): List<Dish> = filter(index(dishes), criteria)

    fun filter(index: List<SearchableDish>, criteria: DishFilterCriteria): List<Dish> {
        val normalizedQuery = criteria.query.normalizedForSearch()
        return index
            .filter { criteria.types.accepts(it.dish.type) }
            .filter { criteria.heaviness.accepts(it.dish.heaviness) }
            .filter { criteria.ratings.accepts(it.dish.rating) }
            .filter { criteria.mealTimes.accepts(it.dish.mealTime) }
            .filter { it.matches(normalizedQuery) }
            .map(SearchableDish::dish)
    }

    private fun <T> Set<T>.accepts(value: T): Boolean = isEmpty() || value in this
}
