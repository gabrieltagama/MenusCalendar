package com.gabrieltagama.menuplanner.feature.calendar.dinner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.ObserveDishesUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ClearDinnerDayUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.GetDinnerDayUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.SaveDinnerDayUseCase
import com.gabrieltagama.menuplanner.feature.calendar.navigation.DinnerEditorRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Editor of the dinner of one day: a single dish chosen among the dinner options (dishes served
 * at dinner or at any time, desserts excluded), picked from a list or at random, different from
 * the current one when possible.
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
    private val random: Random
) : ViewModel() {

    private val date: LocalDate = LocalDate.ofEpochDay(savedStateHandle.toRoute<DinnerEditorRoute>().epochDay)

    private val form = MutableStateFlow(DinnerEditorForm())

    private val options: Flow<List<Dish>> =
        observeDishes().map { dishes -> dishes.filter(Dish::isDinnerOption).sortedBy { it.name.lowercase() } }

    val uiState: StateFlow<DinnerEditorUiState> =
        combine(form, options) { currentForm, currentOptions ->
            DinnerEditorUiState(date = date, form = currentForm, options = currentOptions, isOptionsLoading = false)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DinnerEditorUiState(date = date))

    private val _events = Channel<DinnerEditorEvent>(Channel.BUFFERED)
    val events: Flow<DinnerEditorEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val dinner = getDinnerDay(date)
            form.update { it.copy(isLoading = false, hasSavedDinner = dinner != null, selected = dinner?.dish) }
        }
    }

    fun onDishSelect(dish: Dish?) = form.update { it.copy(selected = dish) }

    fun onRandomDish() {
        val state = uiState.value
        state.options.filterNot { it.id == state.form.selected?.id }.ifEmpty { state.options }.randomOrNull(random)?.let(::onDishSelect)
    }

    fun onClearRequest() = form.update { it.copy(showClearConfirm = true) }

    fun onClearDismiss() = form.update { it.copy(showClearConfirm = false) }

    fun onErrorShown() = form.update { it.copy(error = null) }

    fun onSave() {
        val state = uiState.value
        if (!state.canSave) return
        val dish = state.form.selected ?: return
        form.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (val outcome = saveDinnerDay(date, dish)) {
                is Outcome.Success -> _events.send(DinnerEditorEvent.Close)
                is Outcome.Failure -> form.update { it.copy(isSaving = false, error = outcome.error) }
            }
        }
    }

    fun onClearConfirm() {
        form.update { it.copy(showClearConfirm = false, isSaving = true) }
        viewModelScope.launch {
            clearDinnerDay(date)
            _events.send(DinnerEditorEvent.Close)
        }
    }
}
