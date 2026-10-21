package com.gabrieltagama.menuplanner.core.domain.model

import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import java.time.Instant

/**
 * Aggregate root for a dish of the recipe book; updatedAt decides which version wins when a shared
 * JSON is imported. Its invariants live here: a dish needs a name and valid ingredient lines
 * (validationError), and it can be planned at lunch or as a dinner when it is served at that meal and
 * is not a dessert (isLunchOption, isDinnerOption). The same rules apply when saving, importing and restoring.
 */
data class Dish(
    val id: DishId,
    val name: String,
    val description: String = "",
    val ingredients: List<Ingredient> = emptyList(),
    val preparation: String = "",
    val type: DishType,
    val heaviness: Heaviness,
    val rating: Rating = Rating.THREE,
    val mealTime: MealTime = MealTime.ANY,
    val updatedAt: Instant
) {
    val isLunchOption: Boolean get() = mealTime.isServedAtLunch && type != DishType.DESSERT

    val isDinnerOption: Boolean get() = isDinnerOption(type, mealTime)

    val validationError: DomainError? get() = validate(name, ingredients)

    fun toDraft(): DishDraft = DishDraft(
        id = id,
        name = name,
        description = description,
        ingredients = ingredients,
        preparation = preparation,
        type = type,
        heaviness = heaviness,
        rating = rating,
        mealTime = mealTime
    )

    companion object {
        fun isDinnerOption(type: DishType, mealTime: MealTime): Boolean = mealTime.isServedAtDinner && type != DishType.DESSERT

        fun validate(name: String, ingredients: List<Ingredient>): DomainError? {
            if (name.isBlank()) return DomainError.BlankDishName
            val invalidIndex = ingredients.indexOfFirst { !it.isValid }
            return if (invalidIndex >= 0) DomainError.InvalidIngredient(invalidIndex) else null
        }
    }
}
