package com.gabrieltagama.menuplanner.core.domain.text

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import java.text.Normalizer

/**
 * Text normalization for case- and accent-insensitive search ("Pú" matches "puré"), and the dish
 * search rule shared by the recipe book and the dish picker: the query is contained in the dish
 * name or in the name of any of its ingredients. SearchableDish normalizes those texts once, so a
 * list can be searched on every keystroke without normalizing every dish again.
 */
private val combiningMarks = Regex("\\p{Mn}+")

fun String.normalizedForSearch(): String =
    Normalizer.normalize(trim(), Normalizer.Form.NFD).replace(combiningMarks, "").lowercase()

fun Dish.searchableTexts(): List<String> = listOf(name.normalizedForSearch()) + ingredients.map { it.name.normalizedForSearch() }

fun Dish.matchesSearch(query: String): Boolean = SearchableDish(this).matches(query.normalizedForSearch())

class SearchableDish(val dish: Dish) {
    private val searchableTexts: List<String> = dish.searchableTexts()
    val sortKey: String = searchableTexts.first()

    fun matches(normalizedQuery: String): Boolean = normalizedQuery.isEmpty() || searchableTexts.any { normalizedQuery in it }
}
