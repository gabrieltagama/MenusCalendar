package com.gabrieltagama.menuplanner.feature.dishes.editor

import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit

/**
 * Immutable form state of the dish editor. Quantities are kept as raw text so the user can type
 * freely ("1,5", "0.25"); they are parsed and validated only when saving. A blank quantity is
 * accepted only for the "al gusto" unit (stored as 0). Each ingredient row carries a stable key.
 */
data class DishEditorUiState(
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val name: String = "",
    val description: String = "",
    val type: DishType = DishType.MAIN,
    val heaviness: Heaviness = Heaviness.MEDIUM,
    val ingredients: List<IngredientForm> = emptyList(),
    val preparation: String = "",
    val showValidationErrors: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val error: DomainError? = null
) {
    val isNameInvalid: Boolean get() = showValidationErrors && name.isBlank()
    val canSubmit: Boolean get() = !isLoading && !isSaving
}

data class IngredientForm(
    val key: Long,
    val name: String = "",
    val quantityText: String = "",
    val unit: MeasureUnit = MeasureUnit.GRAM
) {
    val parsedQuantity: Double?
        get() = if (quantityText.isBlank() && unit == MeasureUnit.TO_TASTE) 0.0
        else quantityText.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }

    val isNameValid: Boolean get() = name.isNotBlank()
    val isQuantityValid: Boolean get() = parsedQuantity != null

    fun toDomain(): Ingredient? = parsedQuantity?.let { Ingredient(name = name, quantity = it, unit = unit) }
}

internal fun Ingredient.toForm(key: Long): IngredientForm =
    IngredientForm(key = key, name = name, quantityText = quantity.toEditableText(), unit = unit)

private fun Double.toEditableText(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString().replace('.', ',')
