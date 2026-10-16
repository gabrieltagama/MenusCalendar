package com.gabrieltagama.menuplanner.core.domain.model

import java.text.Collator
import java.util.Locale

/**
 * Orders in which dishes can be listed: by name (Spanish order, ignoring case and accents), by
 * last time served (never served first, then the oldest), by rating (best first) and by
 * heaviness (lightest first). Ties are always broken by name.
 */
enum class DishSort {
    NAME,
    LAST_SERVED,
    RATING,
    HEAVINESS;

    fun sorted(dishes: List<Dish>, usage: DishUsage = DishUsage.NONE): List<Dish> {
        val byName = compareBy(spanishCollator(), Dish::name)
        val primary: Comparator<Dish>? = when (this) {
            NAME -> null
            LAST_SERVED -> compareBy(nullsFirst()) { usage.lastServedOf(it.id) }
            RATING -> compareByDescending(Dish::rating)
            HEAVINESS -> compareBy(Dish::heaviness)
        }
        return dishes.sortedWith(primary?.then(byName) ?: byName)
    }
}

private fun spanishCollator(): Collator = Collator.getInstance(Locale.forLanguageTag("es-ES")).apply { strength = Collator.PRIMARY }
