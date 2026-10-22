package com.gabrieltagama.menuplanner.feature.calendar.dinner

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishUsage
import com.gabrieltagama.menuplanner.feature.calendar.editor.EditorStatus
import java.time.LocalDate

/**
 * State of the dinner editor: the selected dish, the shared EditorStatus, the dinner options and
 * when each dish was last served.
 */
data class DinnerEditorForm(val selected: Dish? = null)

data class DinnerEditorUiState(
    val date: LocalDate,
    val form: DinnerEditorForm = DinnerEditorForm(),
    val status: EditorStatus = EditorStatus(),
    val options: List<Dish> = emptyList(),
    val isOptionsLoading: Boolean = true,
    val usage: DishUsage = DishUsage.NONE
) {
    val canSave: Boolean get() = status.isIdle && form.selected != null
}
