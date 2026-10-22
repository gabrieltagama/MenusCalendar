package com.gabrieltagama.menuplanner.feature.calendar.dinner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gabrieltagama.menuplanner.core.domain.common.DefaultDispatcher
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishSort
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.ObserveDishUsageUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.ObserveDishesUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.DinnerPlanCommands
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.PickRandomDishUseCase
import com.gabrieltagama.menuplanner.feature.calendar.editor.PlanEditorViewModel
import com.gabrieltagama.menuplanner.feature.calendar.navigation.DinnerEditorRoute
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
 * Editor of the dinner of one day: a single dish chosen among the dinner options (dishes served at
 * dinner or at any time, desserts excluded), picked from a list or at random, different from the
 * current one when possible. The options come with when each dish was last served.
 */
@HiltViewModel
class DinnerEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeDishes: ObserveDishesUseCase,
    observeDishUsage: ObserveDishUsageUseCase,
    private val commands: DinnerPlanCommands,
    private val pickRandomDish: PickRandomDishUseCase,
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
) : PlanEditorViewModel<DinnerDay, DinnerEditorForm>(savedStateHandle.toRoute<DinnerEditorRoute>().date, commands, DinnerEditorForm()) {

    private val options: Flow<List<Dish>> =
        observeDishes().map { dishes -> DishSort.NAME.sorted(dishes.filter(Dish::isDinnerOption)) }.flowOn(defaultDispatcher)

    val uiState: StateFlow<DinnerEditorUiState> =
        combine(form, status, options, observeDishUsage()) { currentForm, currentStatus, currentOptions, usage ->
            DinnerEditorUiState(date = date, form = currentForm, status = currentStatus, options = currentOptions, isOptionsLoading = false, usage = usage)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DinnerEditorUiState(date = date))

    init {
        loadSavedDay()
    }

    fun onDishSelect(dish: Dish?) = form.update { it.copy(selected = dish) }

    fun onRandomDish() {
        val state = uiState.value
        pickRandomDish(state.options, state.form.selected)?.let(::onDishSelect)
    }

    fun onSave() {
        val state = uiState.value
        if (!state.canSave) return
        val dish = state.form.selected ?: return
        save { commands.save(date, dish) }
    }

    override fun DinnerEditorForm.filledWith(day: DinnerDay) = copy(selected = day.dish)
}
