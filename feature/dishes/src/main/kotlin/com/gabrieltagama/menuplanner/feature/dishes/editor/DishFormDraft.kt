package com.gabrieltagama.menuplanner.feature.dishes.editor

import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.Rating
import kotlinx.serialization.Serializable

/**
 * Serializable copy of what the user can edit in the dish form, without the ingredient row keys.
 * The editor keeps it in the SavedStateHandle so a recipe being typed survives the system killing
 * the app, and compares it with the copy taken when the form was opened to know whether there are
 * unsaved changes. applyTo puts it back into a form state, giving each ingredient a new key.
 */
@Serializable
internal data class DishFormDraft(
    val name: String,
    val description: String,
    val type: DishType,
    val heaviness: Heaviness,
    val rating: Rating,
    val mealTime: MealTime,
    val ingredients: List<IngredientDraft>,
    val preparation: String
) {
    fun applyTo(state: DishEditorUiState, newKey: () -> Long): DishEditorUiState = state.copy(
        name = name,
        description = description,
        type = type,
        heaviness = heaviness,
        rating = rating,
        mealTime = mealTime,
        ingredients = ingredients.map { IngredientForm(key = newKey(), name = it.name, quantityText = it.quantityText, unit = it.unit) },
        preparation = preparation
    )

    companion object {
        fun of(state: DishEditorUiState): DishFormDraft = DishFormDraft(
            name = state.name,
            description = state.description,
            type = state.type,
            heaviness = state.heaviness,
            rating = state.rating,
            mealTime = state.mealTime,
            ingredients = state.ingredients.map { IngredientDraft(it.name, it.quantityText, it.unit) },
            preparation = state.preparation
        )
    }
}

@Serializable
internal data class IngredientDraft(
    val name: String,
    val quantityText: String,
    val unit: MeasureUnit
)
