package com.gabrieltagama.menuplanner.core.domain.model

import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import java.time.Instant

/**
 * What the user edits before saving a dish. A new dish has no id yet; SaveDishUseCase assigns the
 * identity and the time of the change, so the editor never invents either. validationError applies
 * the Dish rules and toDish is the factory that builds the dish with its texts trimmed.
 */
data class DishDraft(
    val id: DishId? = null,
    val name: String,
    val description: String = "",
    val ingredients: List<Ingredient> = emptyList(),
    val preparation: String = "",
    val type: DishType,
    val heaviness: Heaviness,
    val rating: Rating = Rating.THREE,
    val mealTime: MealTime = MealTime.ANY
) {
    val validationError: DomainError? get() = Dish.validate(name, ingredients)

    fun toDish(id: DishId, updatedAt: Instant): Dish = Dish(
        id = id,
        name = name.trim(),
        description = description.trim(),
        ingredients = ingredients.map { it.copy(name = it.name.trim()) },
        preparation = preparation.trim(),
        type = type,
        heaviness = heaviness,
        rating = rating,
        mealTime = mealTime,
        updatedAt = updatedAt
    )
}
