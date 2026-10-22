package com.gabrieltagama.menuplanner.feature.dishes.list

import com.gabrieltagama.menuplanner.core.domain.model.CatalogDish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.model.Rating
import com.gabrieltagama.menuplanner.core.domain.text.SearchableDish
import com.gabrieltagama.menuplanner.core.domain.text.normalizedForSearch

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

internal object DishFilter {

    fun <T : CatalogDish> index(dishes: List<T>): List<SearchableDish<T>> = dishes.map { SearchableDish(it) }.sortedBy { it.sortKey }

    fun <T : CatalogDish> filter(index: List<SearchableDish<T>>, criteria: DishFilterCriteria): List<T> {
        val normalizedQuery = criteria.query.normalizedForSearch()
        return index
            .filter { criteria.types.accepts(it.dish.type) }
            .filter { criteria.heaviness.accepts(it.dish.heaviness) }
            .filter { criteria.ratings.accepts(it.dish.rating) }
            .filter { criteria.mealTimes.accepts(it.dish.mealTime) }
            .filter { it.matches(normalizedQuery) }
            .map { it.dish }
    }

    private fun <T> Set<T>.accepts(value: T): Boolean = isEmpty() || value in this
}
