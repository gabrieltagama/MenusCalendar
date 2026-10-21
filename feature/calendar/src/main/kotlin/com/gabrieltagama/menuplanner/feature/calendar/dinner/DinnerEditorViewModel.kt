package com.gabrieltagama.menuplanner.feature.calendar.dinner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishSort
import com.gabrieltagama.menuplanner.core.domain.model.DishUsage
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.ObserveDishUsageUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.ObserveDishesUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ClearDinnerDayUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.GetDinnerDayUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.PickRandomDishUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.SaveDinnerDayUseCase
import com.gabrieltagama.menuplanner.feature.calendar.editor.PlanEditorViewModel
import com.gabrieltagama.menuplanner.feature.calendar.navigation.DinnerEditorRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Editor of the dinner of one day: a single dish chosen among the dinner options (dishes served
 * at dinner or at any time, desserts excluded), picked from a list or at random, different from
 * the current one when possible. The options come with when each dish was last served.
 */
data class DinnerEditorForm(
    val selected: Dish? = null,
    val hasSavedDinner: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val showClearConfirm: Boolean = false,
    val error: DomainError? = null
)

data class DinnerEditorUiState(
    val date: LocalDate,
    val form: DinnerEditorForm = DinnerEditorForm(),
    val options: List<Dish> = emptyList(),
    val isOptionsLoading: Boolean = true,
    val usage: DishUsage = DishUsage.NONE
) {
    val canSave: Boolean get() = !form.isLoading && !form.isSaving && form.selected != null
}

@HiltViewModel
class DinnerEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeDishes: ObserveDishesUseCase,
    observeDishUsage: ObserveDishUsageUseCase,
    private val getDinnerDay: GetDinnerDayUseCase,
    private val saveDinnerDay: SaveDinnerDayUseCase,
    private val clearDinnerDay: ClearDinnerDayUseCase,
    private val pickRandomDish: PickRandomDishUseCase
) : PlanEditorViewModel<DinnerEditorForm>(DinnerEditorForm()) {

    private val date: LocalDate = LocalDate.ofEpochDay(savedStateHandle.toRoute<DinnerEditorRoute>().epochDay)

    private val options: Flow<List<Dish>> =
        observeDishes().map { dishes -> DishSort.NAME.sorted(dishes.filter(Dish::isDinnerOption)) }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<DinnerEditorUiState> =
        combine(form, options, observeDishUsage()) { currentForm, currentOptions, usage ->
            DinnerEditorUiState(date = date, form = currentForm, options = currentOptions, isOptionsLoading = false, usage = usage)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DinnerEditorUiState(date = date))

    init {
        viewModelScope.launch {
            val dinner = getDinnerDay(date)
            form.update { it.copy(isLoading = false, hasSavedDinner = dinner != null, selected = dinner?.dish) }
        }
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
        save { saveDinnerDay(date, dish) }
    }

    override fun DinnerEditorForm.withClearConfirm(visible: Boolean) = copy(showClearConfirm = visible)

    override fun DinnerEditorForm.withSaving(saving: Boolean) = copy(isSaving = saving)

    override fun DinnerEditorForm.withError(error: DomainError?) = copy(error = error)

    override suspend fun clearDay() = clearDinnerDay(date)
}
