package com.gabrieltagama.menuplanner.feature.calendar.editor

import com.gabrieltagama.menuplanner.core.domain.common.DomainError

/**
 * Progress shared by the lunch and dinner editors: whether the saved day is still loading and
 * exists, a save or clear in progress, the clear confirmation and the error to show once. isIdle
 * tells that the editor accepts a save. PlanEditorEvent.Close asks the screen to close the editor.
 */
data class EditorStatus(
    val isLoading: Boolean = true,
    val hasSavedPlan: Boolean = false,
    val isSaving: Boolean = false,
    val showClearConfirm: Boolean = false,
    val error: DomainError? = null
) {
    val isIdle: Boolean get() = !isLoading && !isSaving
}

sealed interface PlanEditorEvent {
    data object Close : PlanEditorEvent
}
