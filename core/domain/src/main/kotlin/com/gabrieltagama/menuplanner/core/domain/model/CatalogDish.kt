package com.gabrieltagama.menuplanner.core.domain.model

/**
 * What the recipe book lists and searches of a dish: its identity, name, classification and the
 * names of its ingredients. The Dish aggregate and its read model DishListItem both provide it, so
 * DishSort and SearchableDish order and search the recipe book list and the dish pickers the same way.
 */
interface CatalogDish {
    val id: DishId
    val name: String
    val type: DishType
    val heaviness: Heaviness
    val rating: Rating
    val mealTime: MealTime
    val ingredientNames: List<String>
}
