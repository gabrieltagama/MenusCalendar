package com.gabrieltagama.menuplanner.core.domain.model

import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.RecipeError
import java.time.Instant

/**
 * Aggregate root for a dish of the recipe book; updatedAt decides which version wins when a shared
 * JSON is imported. Its invariants live here and are checked when a dish is created, so an invalid
 * dish cannot exist: it needs a valid name (DishName) and valid ingredient lines (validate, which the
 * editor and the imports apply first to report the error). It can be planned at lunch or as a dinner
 * when it is served at that meal and is not a dessert (isLunchOption, isDinnerOption). The same rules apply when saving, importing and restoring,
 * with one exception: a lunch chosen by hand may take a dish meant for dinner (the editor only warns), so
 * only the autofill applies isLunchOption strictly.
 */
data class Dish(
    override val id: DishId,
    override val name: String,
    val description: String = "",
    val ingredients: List<Ingredient> = emptyList(),
    val preparation: String = "",
    override val type: DishType,
    override val heaviness: Heaviness,
    override val rating: Rating = Rating.THREE,
    override val mealTime: MealTime = MealTime.ANY,
    val updatedAt: Instant
) : CatalogDish {
    init {
        require(validate(name, ingredients) == null) { "A dish needs a name and valid ingredient lines" }
    }

    override val ingredientNames: List<String> get() = ingredients.map(Ingredient::name)

    val isLunchOption: Boolean get() = mealTime.isServedAtLunch && type != DishType.DESSERT

    val isDinnerOption: Boolean get() = isDinnerOption(type, mealTime)

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
            if (!DishName.isValid(name)) return RecipeError.BlankDishName
            val invalidIndex = ingredients.indexOfFirst { !it.isValid }
            return if (invalidIndex >= 0) RecipeError.InvalidIngredient(invalidIndex) else null
        }
    }
}
