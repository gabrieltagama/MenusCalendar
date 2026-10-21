package com.gabrieltagama.menuplanner.feature.calendar.dinner

import com.gabrieltagama.menuplanner.core.domain.model.Dish

/**
 * Callbacks of the dinner editor screen, one per user action, so the stateless DinnerEditorScreen
 * can be previewed and tested without a ViewModel.
 */
data class DinnerEditorActions(
    val onBack: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onDishSelect: (Dish?) -> Unit = {},
    val onRandomDish: () -> Unit = {},
    val onClearRequest: () -> Unit = {},
    val onClearConfirm: () -> Unit = {},
    val onClearDismiss: () -> Unit = {}
)
