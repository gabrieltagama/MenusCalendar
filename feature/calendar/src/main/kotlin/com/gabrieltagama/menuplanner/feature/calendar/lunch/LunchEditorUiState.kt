package com.gabrieltagama.menuplanner.feature.calendar.lunch

import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.DishUsage
import com.gabrieltagama.menuplanner.core.domain.model.MenuShape
import com.gabrieltagama.menuplanner.core.domain.model.MenuSlot
import com.gabrieltagama.menuplanner.feature.calendar.editor.EditorStatus
import java.time.LocalDate

/**
 * State of the lunch editor: the form (menu shape and the dish picked for every slot), the shared
 * EditorStatus, the options of each dish type and when each dish was last served. toMenu builds the
 * menu of the active shape with DailyMenu.of, so only those slots are saved.
 */
data class LunchEditorForm(
    val mode: MenuShape = MenuShape.COURSES,
    val selections: Map<MenuSlot, Dish> = emptyMap()
)

data class LunchEditorUiState(
    val date: LocalDate,
    val form: LunchEditorForm = LunchEditorForm(),
    val status: EditorStatus = EditorStatus(),
    val options: Map<DishType, List<Dish>> = emptyMap(),
    val isOptionsLoading: Boolean = true,
    val usage: DishUsage = DishUsage.NONE
) {
    val visibleSlots: List<MenuSlot> get() = form.mode.slots

    val canSave: Boolean get() = status.isIdle && toMenu() != null

    fun optionsFor(slot: MenuSlot): List<Dish> = options[slot.dishType].orEmpty()

    fun selectionFor(slot: MenuSlot): Dish? = form.selections[slot]

    fun toMenu(): DailyMenu? = DailyMenu.of(form.mode, form.selections)
}
