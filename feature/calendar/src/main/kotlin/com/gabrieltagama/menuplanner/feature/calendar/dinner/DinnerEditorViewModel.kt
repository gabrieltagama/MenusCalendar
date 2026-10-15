package com.gabrieltagama.menuplanner.feature.calendar.dinner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.ObserveDishesUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ClearDinnerDayUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.GetDinnerDayUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.SaveDinnerDayUseCase
import com.gabrieltagama.menuplanner.feature.calendar.common.EditorForm
import com.gabrieltagama.menuplanner.feature.calendar.common.PlanEditorViewModel
import com.gabrieltagama.menuplanner.feature.calendar.navigation.DinnerEditorRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Editor of the dinner of one day: a single dish chosen among the dinner options (dishes served
 * at dinner or at any time, desserts excluded, in Spanish order), picked from a list or at
 * random. Saving, clearing and the random pick are shared with the lunch editor
 * (PlanEditorViewModel).
 */
data class DinnerEditorForm(
    val selected: Dish? = null,
    val hasSavedDinner: Boolean = false,
    override val isLoading: Boolean = true,
    override val isSaving: Boolean = false,
    override val showClearConfirm: Boolean = false,
    override val error: DomainError? = null
) : EditorForm<DinnerEditorForm> {
    override fun withStatus(isSaving: Boolean, showClearConfirm: Boolean, error: DomainError?): DinnerEditorForm =
        copy(isSaving = isSaving, showClearConfirm = showClearConfirm, error = error)
}

data class DinnerEditorUiState(
    val date: LocalDate,
    val form: DinnerEditorForm = DinnerEditorForm(),
    val options: List<Dish> = emptyList(),
    val isOptionsLoading: Boolean = true
) {
    val canSave: Boolean get() = !form.isLoading && !form.isSaving && form.selected != null
}

sealed interface DinnerEditorEvent {
    data object Close : DinnerEditorEvent
}

@HiltViewModel
class DinnerEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeDishes: ObserveDishesUseCase,
    private val getDinnerDay: GetDinnerDayUseCase,
    private val saveDinnerDay: SaveDinnerDayUseCase,
    private val clearDinnerDay: ClearDinnerDayUseCase,
    random: Random
) : PlanEditorViewModel<DinnerEditorForm, DinnerEditorEvent>(DinnerEditorForm(), DinnerEditorEvent.Close, random) {

    private val date: LocalDate = LocalDate.ofEpochDay(savedStateHandle.toRoute<DinnerEditorRoute>().epochDay)

    private val options: Flow<List<Dish>> = observeDishes().map { dishes -> dishes.filter(Dish::isDinnerOption) }

    val uiState: StateFlow<DinnerEditorUiState> =
        combine(form, options) { currentForm, currentOptions ->
            DinnerEditorUiState(date = date, form = currentForm, options = currentOptions, isOptionsLoading = false)
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
        randomDish(state.options, state.form.selected)?.let(::onDishSelect)
    }

    fun onSave() {
        val state = uiState.value
        if (!state.canSave) return
        val dish = state.form.selected ?: return
        saveDay { saveDinnerDay(date, dish) }
    }

    override suspend fun clearDay() = clearDinnerDay(date)
}
