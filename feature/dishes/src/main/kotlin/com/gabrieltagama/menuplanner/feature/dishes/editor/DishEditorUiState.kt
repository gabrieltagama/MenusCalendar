package com.gabrieltagama.menuplanner.feature.dishes.editor

import com.gabrieltagama.menuplanner.core.domain.common.DomainError

/**
 * Immutable state of the dish editor: the DishForm being edited and the progress around it
 * (loading, saving, the validation errors shown after a first save attempt, the delete and discard
 * confirmations and the error to show once). DishEditorEvent.Close asks the screen to close it.
 */
data class DishEditorUiState(
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val form: DishForm = DishForm(),
    val showValidationErrors: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val showDiscardConfirm: Boolean = false,
    val error: DomainError? = null
) {
    val isNameInvalid: Boolean get() = showValidationErrors && !form.isNameValid
    val canSubmit: Boolean get() = !isLoading && !isSaving
}

sealed interface DishEditorEvent {
    data object Close : DishEditorEvent
}
