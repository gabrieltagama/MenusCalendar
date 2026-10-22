package com.gabrieltagama.menuplanner.core.domain.model

import com.gabrieltagama.menuplanner.core.domain.text.spanishCollator

/**
 * Orders in which dishes can be listed: by name (Spanish order, ignoring case and accents), by
 * last time served (never served first, then the oldest), by rating (best first) and by
 * heaviness (lightest first). Ties are always broken by name. It orders any CatalogDish, so the
 * recipe book list and the dish pickers share it.
 */
enum class DishSort {
    NAME,
    LAST_SERVED,
    RATING,
    HEAVINESS;

    fun <T : CatalogDish> sorted(dishes: List<T>, usage: DishUsage = DishUsage.NONE): List<T> {
        val byName: Comparator<T> = compareBy(spanishCollator()) { it.name }
        val primary: Comparator<T>? = when (this) {
            NAME -> null
            LAST_SERVED -> compareBy(nullsFirst()) { usage.lastServedOf(it.id) }
            RATING -> compareByDescending { it.rating }
            HEAVINESS -> compareBy { it.heaviness }
        }
        return dishes.sortedWith(primary?.then(byName) ?: byName)
    }
}
