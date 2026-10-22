package com.gabrieltagama.menuplanner.core.domain.text

import com.gabrieltagama.menuplanner.core.domain.model.CatalogDish

/**
 * Text normalization for case- and accent-insensitive search ("Pú" matches "puré"), and the dish
 * search rule shared by the recipe book and the dish picker: the query is contained in the dish
 * name or in the name of any of its ingredients. SearchableDish normalizes those texts once, so a
 * list can be searched on every keystroke without normalizing every dish again; it works on any
 * CatalogDish (the Dish aggregate or the DishListItem read model).
 */
fun String.normalizedForSearch(): String = trim().foldedCaseAndAccents()

fun CatalogDish.searchableTexts(): List<String> = listOf(name.normalizedForSearch()) + ingredientNames.map { it.normalizedForSearch() }

class SearchableDish<T : CatalogDish>(val dish: T) {
    private val searchableTexts: List<String> = dish.searchableTexts()
    val sortKey: String = searchableTexts.first()

    fun matches(normalizedQuery: String): Boolean = normalizedQuery.isEmpty() || searchableTexts.any { normalizedQuery in it }
}
