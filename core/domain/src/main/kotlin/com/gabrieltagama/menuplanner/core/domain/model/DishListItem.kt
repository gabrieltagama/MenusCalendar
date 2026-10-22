package com.gabrieltagama.menuplanner.core.domain.model

/**
 * Read model of a dish in the recipe book list: what the list shows, filters and searches, without
 * description, preparation or quantities. It is a query result, never saved back; commands use Dish.
 */
data class DishListItem(
    override val id: DishId,
    override val name: String,
    override val type: DishType,
    override val heaviness: Heaviness,
    override val rating: Rating,
    override val mealTime: MealTime,
    override val ingredientNames: List<String>
) : CatalogDish

fun Dish.toListItem(): DishListItem =
    DishListItem(id = id, name = name, type = type, heaviness = heaviness, rating = rating, mealTime = mealTime, ingredientNames = ingredientNames)
