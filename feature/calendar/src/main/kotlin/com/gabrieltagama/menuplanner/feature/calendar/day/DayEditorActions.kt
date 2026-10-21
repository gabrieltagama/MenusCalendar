package com.gabrieltagama.menuplanner.feature.calendar.day

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.MenuShape
import com.gabrieltagama.menuplanner.core.domain.model.MenuSlot

/**
 * Callbacks of the lunch editor screen, one per user action, so the stateless DayEditorScreen can
 * be previewed and tested without a ViewModel.
 */
data class DayEditorActions(
    val onBack: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onModeChange: (MenuShape) -> Unit = {},
    val onDishSelect: (MenuSlot, Dish?) -> Unit = { _, _ -> },
    val onRandomDish: (MenuSlot) -> Unit = {},
    val onClearRequest: () -> Unit = {},
    val onClearConfirm: () -> Unit = {},
    val onClearDismiss: () -> Unit = {}
)
