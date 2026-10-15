package com.gabrieltagama.menuplanner.core.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Aggregate root for a dish of the recipe book. The id is a UUID so dishes can be merged
 * across devices when a shared JSON is imported; updatedAt decides which version wins. A dish can
 * be planned as a dinner when it is served at dinner and is not a dessert; the same rule is exposed
 * as isDinnerOption(type, mealTime) for code that only knows those two attributes (imports).
 */
data class Dish(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val ingredients: List<Ingredient> = emptyList(),
    val preparation: String = "",
    val type: DishType,
    val heaviness: Heaviness,
    val rating: Rating = Rating.THREE,
    val mealTime: MealTime = MealTime.ANY,
    val updatedAt: Instant = Instant.now()
) {
    val isDinnerOption: Boolean get() = isDinnerOption(type, mealTime)

    companion object {
        fun isDinnerOption(type: DishType, mealTime: MealTime): Boolean = mealTime.isServedAtDinner && type != DishType.DESSERT
    }
}
