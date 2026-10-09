package com.gabrieltagama.menuplanner.feature.calendar.day

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.MenuSlot
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.ObserveDishesUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ClearMealDayUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.GetMealDayUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.SaveMealDayUseCase
import com.gabrieltagama.menuplanner.feature.calendar.navigation.DayEditorRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Editor of the lunch of one day. The form keeps a selection for every slot, so switching
 * between "starter + main" and "single dish" never loses what was picked; only the slots of the
 * active mode are saved. Options are observed per dish type.
 */
enum class MenuMode { COURSES, SINGLE }

data class DayEditorForm(
    val mode: MenuMode = MenuMode.COURSES,
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
    val isOptionsLoading: Boolean = true
) {
    val visibleSlots: List<MenuSlot>
        get() = when (form.mode) {
            MenuMode.COURSES -> listOf(MenuSlot.STARTER, MenuSlot.MAIN, MenuSlot.DESSERT)
            MenuMode.SINGLE -> listOf(MenuSlot.SINGLE, MenuSlot.DESSERT)
        }

    val canSave: Boolean get() = !form.isLoading && !form.isSaving && toMenu() != null

    fun optionsFor(slot: MenuSlot): List<Dish> = options[slot.dishType].orEmpty()

    fun selectionFor(slot: MenuSlot): Dish? = form.selections[slot]

    fun toMenu(): DailyMenu? {
        val selections = form.selections
        val dessert = selections[MenuSlot.DESSERT]
        return when (form.mode) {
            MenuMode.COURSES -> selections[MenuSlot.STARTER]?.let { starter ->
                selections[MenuSlot.MAIN]?.let { main -> DailyMenu.Courses(starter = starter, main = main, dessert = dessert) }
            }
            MenuMode.SINGLE -> selections[MenuSlot.SINGLE]?.let { DailyMenu.Single(single = it, dessert = dessert) }
        }
    }
}

val MenuSlot.dishType: DishType
    get() = when (this) {
        MenuSlot.STARTER -> DishType.STARTER
        MenuSlot.MAIN -> DishType.MAIN
        MenuSlot.SINGLE -> DishType.SINGLE
        MenuSlot.DESSERT -> DishType.DESSERT
    }

val MenuSlot.isRequired: Boolean get() = this != MenuSlot.DESSERT

sealed interface DayEditorEvent {
    data object Close : DayEditorEvent
}

@HiltViewModel
class DayEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeDishes: ObserveDishesUseCase,
    private val getMealDay: GetMealDayUseCase,
    private val saveMealDay: SaveMealDayUseCase,
    private val clearMealDay: ClearMealDayUseCase
) : ViewModel() {

    private val date: LocalDate = LocalDate.ofEpochDay(savedStateHandle.toRoute<DayEditorRoute>().epochDay)

    private val form = MutableStateFlow(DayEditorForm())

    private val options: Flow<Map<DishType, List<Dish>>> =
        combine(DishType.entries.map { type -> observeDishes(type) }) { lists ->
            DishType.entries.zip(lists) { type, dishes -> type to dishes.sortedBy { it.name.lowercase() } }.toMap()
        }

    val uiState: StateFlow<DayEditorUiState> =
        combine(form, options) { currentForm, currentOptions ->
            DayEditorUiState(date = date, form = currentForm, options = currentOptions, isOptionsLoading = false)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DayEditorUiState(date = date))

    private val _events = Channel<DayEditorEvent>(Channel.BUFFERED)
    val events: Flow<DayEditorEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch { fillForm(getMealDay(date)) }
    }

    fun onModeChange(mode: MenuMode) = form.update { it.copy(mode = mode) }

    fun onDishSelect(slot: MenuSlot, dish: Dish?) = form.update { current ->
        current.copy(selections = dish?.let { current.selections + (slot to it) } ?: (current.selections - slot))
    }

    fun onClearRequest() = form.update { it.copy(showClearConfirm = true) }

    fun onClearDismiss() = form.update { it.copy(showClearConfirm = false) }

    fun onErrorShown() = form.update { it.copy(error = null) }

    fun onSave() {
        val state = uiState.value
        if (!state.canSave) return
        val menu = state.toMenu() ?: return
        form.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (val outcome = saveMealDay(date, menu)) {
                is Outcome.Success -> _events.send(DayEditorEvent.Close)
                is Outcome.Failure -> form.update { it.copy(isSaving = false, error = outcome.error) }
            }
        }
    }

    fun onClearConfirm() {
        form.update { it.copy(showClearConfirm = false, isSaving = true) }
        viewModelScope.launch {
            clearMealDay(date)
            _events.send(DayEditorEvent.Close)
        }
    }

    private fun fillForm(day: MealDay?) = form.update { current ->
        when (val menu = day?.menu) {
            null -> current.copy(isLoading = false)
            is DailyMenu.Courses -> current.copy(
                isLoading = false,
                hasSavedDay = true,
                mode = MenuMode.COURSES,
                selections = selectionsOf(MenuSlot.STARTER to menu.starter, MenuSlot.MAIN to menu.main, MenuSlot.DESSERT to menu.dessert)
            )
            is DailyMenu.Single -> current.copy(
                isLoading = false,
                hasSavedDay = true,
                mode = MenuMode.SINGLE,
                selections = selectionsOf(MenuSlot.SINGLE to menu.single, MenuSlot.DESSERT to menu.dessert)
            )
        }
    }

    private fun selectionsOf(vararg slots: Pair<MenuSlot, Dish?>): Map<MenuSlot, Dish> =
        slots.mapNotNull { (slot, dish) -> dish?.let { slot to it } }.toMap()
}
