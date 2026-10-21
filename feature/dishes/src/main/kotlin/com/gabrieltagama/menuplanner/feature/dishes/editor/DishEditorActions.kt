package com.gabrieltagama.menuplanner.feature.dishes.editor

import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.Rating

/**
 * Callbacks of the dish editor screen, one per user action, so the stateless DishEditorScreen can
 * be previewed and tested without a ViewModel.
 */
data class DishEditorActions(
    val onBack: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onDeleteRequest: () -> Unit = {},
    val onDeleteConfirm: () -> Unit = {},
    val onDeleteDismiss: () -> Unit = {},
    val onDiscardConfirm: () -> Unit = {},
    val onDiscardDismiss: () -> Unit = {},
    val onNameChange: (String) -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val onTypeChange: (DishType) -> Unit = {},
    val onHeavinessChange: (Heaviness) -> Unit = {},
    val onRatingChange: (Rating) -> Unit = {},
    val onMealTimeChange: (MealTime) -> Unit = {},
    val onPreparationChange: (String) -> Unit = {},
    val onAddIngredient: () -> Unit = {},
    val onRemoveIngredient: (Long) -> Unit = {},
    val onIngredientNameChange: (Long, String) -> Unit = { _, _ -> },
    val onIngredientQuantityChange: (Long, String) -> Unit = { _, _ -> },
    val onIngredientUnitChange: (Long, MeasureUnit) -> Unit = { _, _ -> }
)
