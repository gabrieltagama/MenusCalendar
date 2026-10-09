package com.gabrieltagama.menuplanner.feature.dishes.list

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness

/**
 * Pure filtering of the recipe book table. A dish is kept when it matches the optional type and
 * heaviness and when the case- and accent-insensitive query is contained in its name or in the
 * name of any of its ingredients. Results are sorted alphabetically ignoring accents and case.
 */
data class DishFilterCriteria(
    val query: String = "",
    val type: DishType? = null,
    val heaviness: Heaviness? = null
)

internal object DishFilter {

    fun apply(dishes: List<Dish>, criteria: DishFilterCriteria): List<Dish> {
        val normalizedQuery = criteria.query.normalizedForSearch()
        return dishes
            .filter { criteria.type == null || it.type == criteria.type }
            .filter { criteria.heaviness == null || it.heaviness == criteria.heaviness }
            .filter { it.matches(normalizedQuery) }
            .sortedBy { it.name.normalizedForSearch() }
    }

    private fun Dish.matches(normalizedQuery: String): Boolean =
        normalizedQuery.isEmpty() ||
            name.containsNormalized(normalizedQuery) ||
            ingredients.any { it.name.containsNormalized(normalizedQuery) }
}
