package com.gabrieltagama.menuplanner.core.domain.model

/**
 * What the user edits before saving a dish. A new dish has no id yet; SaveDishUseCase assigns the
 * identity and the time of the change, so the editor never invents either.
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
)
