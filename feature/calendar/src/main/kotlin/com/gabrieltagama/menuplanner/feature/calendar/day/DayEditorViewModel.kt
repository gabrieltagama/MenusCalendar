package com.gabrieltagama.menuplanner.feature.calendar.day

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gabrieltagama.menuplanner.core.domain.common.DefaultDispatcher
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishSort
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.DishUsage
import com.gabrieltagama.menuplanner.core.domain.model.LunchDay
import com.gabrieltagama.menuplanner.core.domain.model.MenuShape
import com.gabrieltagama.menuplanner.core.domain.model.MenuSlot
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.ObserveDishUsageUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.ObserveDishesUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ClearLunchDayUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.GetLunchDayUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.PickRandomDishUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.SaveLunchDayUseCase
import com.gabrieltagama.menuplanner.feature.calendar.editor.PlanEditorViewModel
import com.gabrieltagama.menuplanner.feature.calendar.navigation.DayEditorRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
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
import kotlinx.coroutines.launch

/**
 * Editor of the lunch of one day. The form keeps a selection for every slot, so switching
 * between "starter + main" and "single dish" never loses what was picked; only the slots of the
 * active mode are saved. Options come from a single observation of the recipe book grouped by dish
 * type, together with when each dish was last served. A slot can also be filled with a random
 * dish of its type, different from the current one when possible.
 */
data class DayEditorForm(
    val mode: MenuShape = MenuShape.COURSES,
    val selections: Map<MenuSlot, Dish> = emptyMap(),
    val hasSavedDay: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val showClearConfirm: Boolean = false,
    val error: DomainError? = null
)

data class DayEditorUiState(
    val date: LocalDate,
    val form: DayEditorForm = DayEditorForm(),
    val options: Map<DishType, List<Dish>> = emptyMap(),
    val isOptionsLoading: Boolean = true,
    val usage: DishUsage = DishUsage.NONE
) {
    val visibleSlots: List<MenuSlot> get() = form.mode.slots

    val canSave: Boolean get() = !form.isLoading && !form.isSaving && toMenu() != null

    fun optionsFor(slot: MenuSlot): List<Dish> = options[slot.dishType].orEmpty()

    fun selectionFor(slot: MenuSlot): Dish? = form.selections[slot]

    fun toMenu(): DailyMenu? = DailyMenu.of(form.mode, form.selections)
}

@HiltViewModel
class DayEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeDishes: ObserveDishesUseCase,
    observeDishUsage: ObserveDishUsageUseCase,
    private val getLunchDay: GetLunchDayUseCase,
    private val saveLunchDay: SaveLunchDayUseCase,
    private val clearLunchDay: ClearLunchDayUseCase,
    private val pickRandomDish: PickRandomDishUseCase,
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
) : PlanEditorViewModel<DayEditorForm>(DayEditorForm()) {

    private val date: LocalDate = LocalDate.ofEpochDay(savedStateHandle.toRoute<DayEditorRoute>().epochDay)

    private val plannableTypes: Set<DishType> = MenuSlot.entries.map { it.dishType }.toSet()

    private val options: Flow<Map<DishType, List<Dish>>> =
        observeDishes().map { dishes ->
            DishSort.NAME.sorted(dishes.filter { it.type in plannableTypes }).groupBy(Dish::type)
        }.flowOn(defaultDispatcher)

    val uiState: StateFlow<DayEditorUiState> =
        combine(form, options, observeDishUsage()) { currentForm, currentOptions, usage ->
            DayEditorUiState(date = date, form = currentForm, options = currentOptions, isOptionsLoading = false, usage = usage)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DayEditorUiState(date = date))

    init {
        viewModelScope.launch { fillForm(getLunchDay(date)) }
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
        save { saveLunchDay(date, menu) }
    }

    override fun DayEditorForm.withClearConfirm(visible: Boolean) = copy(showClearConfirm = visible)

    override fun DayEditorForm.withSaving(saving: Boolean) = copy(isSaving = saving)

    override fun DayEditorForm.withError(error: DomainError?) = copy(error = error)

    override suspend fun clearDay() = clearLunchDay(date)

    private fun fillForm(day: LunchDay?) = form.update { current ->
        val menu = day?.menu ?: return@update current.copy(isLoading = false)
        current.copy(isLoading = false, hasSavedDay = true, mode = menu.shape, selections = menu.slots.toMap())
    }
}
