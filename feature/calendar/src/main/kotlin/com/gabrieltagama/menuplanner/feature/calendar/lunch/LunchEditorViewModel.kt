package com.gabrieltagama.menuplanner.feature.calendar.lunch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gabrieltagama.menuplanner.core.domain.common.DefaultDispatcher
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishSort
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.LunchDay
import com.gabrieltagama.menuplanner.core.domain.model.MenuShape
import com.gabrieltagama.menuplanner.core.domain.model.MenuSlot
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.ObserveDishUsageUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.ObserveDishesUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.LunchPlanCommands
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.PickRandomDishUseCase
import com.gabrieltagama.menuplanner.feature.calendar.editor.PlanEditorViewModel
import com.gabrieltagama.menuplanner.feature.calendar.navigation.LunchEditorRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Editor of the lunch of one day. The form keeps a selection for every slot, so switching between
 * "starter + main" and "single dish" never loses what was picked; only the slots of the active mode
 * are saved. Options come from one observation of the recipe book grouped by dish type, together
 * with when each dish was last served. A slot can also be filled with a random dish of its type,
 * different from the current one when possible.
 */
@HiltViewModel
class LunchEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeDishes: ObserveDishesUseCase,
    observeDishUsage: ObserveDishUsageUseCase,
    private val commands: LunchPlanCommands,
    private val pickRandomDish: PickRandomDishUseCase,
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
) : PlanEditorViewModel<LunchDay, LunchEditorForm>(savedStateHandle.toRoute<LunchEditorRoute>().date, commands, LunchEditorForm()) {

    private val plannableTypes: Set<DishType> = MenuSlot.entries.map { it.dishType }.toSet()

    private val options: Flow<Map<DishType, List<Dish>>> =
        observeDishes().map { dishes ->
            DishSort.NAME.sorted(dishes.filter { it.type in plannableTypes }).groupBy(Dish::type)
        }.flowOn(defaultDispatcher)

    val uiState: StateFlow<LunchEditorUiState> =
        combine(form, status, options, observeDishUsage()) { currentForm, currentStatus, currentOptions, usage ->
            LunchEditorUiState(date = date, form = currentForm, status = currentStatus, options = currentOptions, isOptionsLoading = false, usage = usage)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LunchEditorUiState(date = date))

    init {
        loadSavedDay()
    }

    fun onModeChange(mode: MenuShape) = form.update { it.copy(mode = mode) }

    fun onDishSelect(slot: MenuSlot, dish: Dish?) = form.update { current ->
        current.copy(selections = dish?.let { current.selections + (slot to it) } ?: (current.selections - slot))
    }

    fun onRandomDish(slot: MenuSlot) {
        val state = uiState.value
        pickRandomDish(state.optionsFor(slot), state.selectionFor(slot))?.let { onDishSelect(slot, it) }
    }

    fun onSave() {
        val state = uiState.value
        if (!state.canSave) return
        val menu = state.toMenu() ?: return
        save { commands.save(date, menu) }
    }

    override fun LunchEditorForm.filledWith(day: LunchDay) = copy(mode = day.menu.shape, selections = day.menu.slots.toMap())
}
