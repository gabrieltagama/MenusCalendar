package com.gabrieltagama.menuplanner.feature.calendar.day

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.model.MenuSlot
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.ObserveDishesUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ClearMealDayUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.GetMealDayUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.SaveMealDayUseCase
import com.gabrieltagama.menuplanner.feature.calendar.common.EditorForm
import com.gabrieltagama.menuplanner.feature.calendar.common.PlanEditorViewModel
import com.gabrieltagama.menuplanner.feature.calendar.navigation.DayEditorRoute
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
 * Editor of the lunch of one day. The form keeps a selection for every slot, so switching
 * between "starter + main" and "single dish" never loses what was picked; only the slots of the
 * active mode are saved. Options come from one dish flow grouped by type, already in Spanish
 * order. Saving, clearing and the random pick are shared with the dinner editor
 * (PlanEditorViewModel).
 */
enum class MenuMode { COURSES, SINGLE }

data class DayEditorForm(
    val mode: MenuMode = MenuMode.COURSES,
    val selections: Map<MenuSlot, Dish> = emptyMap(),
    val hasSavedDay: Boolean = false,
    override val isLoading: Boolean = true,
    override val isSaving: Boolean = false,
    override val showClearConfirm: Boolean = false,
    override val error: DomainError? = null
) : EditorForm<DayEditorForm> {
    override fun withStatus(isSaving: Boolean, showClearConfirm: Boolean, error: DomainError?): DayEditorForm =
        copy(isSaving = isSaving, showClearConfirm = showClearConfirm, error = error)
}

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

sealed interface DayEditorEvent {
    data object Close : DayEditorEvent
}

@HiltViewModel
class DayEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeDishes: ObserveDishesUseCase,
    private val getMealDay: GetMealDayUseCase,
    private val saveMealDay: SaveMealDayUseCase,
    private val clearMealDay: ClearMealDayUseCase,
    random: Random
) : PlanEditorViewModel<DayEditorForm, DayEditorEvent>(DayEditorForm(), DayEditorEvent.Close, random) {

    private val date: LocalDate = LocalDate.ofEpochDay(savedStateHandle.toRoute<DayEditorRoute>().epochDay)

    private val options: Flow<Map<DishType, List<Dish>>> = observeDishes().map { dishes -> dishes.groupBy(Dish::type) }

    val uiState: StateFlow<DayEditorUiState> =
        combine(form, options) { currentForm, currentOptions ->
            DayEditorUiState(date = date, form = currentForm, options = currentOptions, isOptionsLoading = false)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DayEditorUiState(date = date))

    init {
        viewModelScope.launch { fillForm(getMealDay(date)) }
    }

    fun onModeChange(mode: MenuMode) = form.update { it.copy(mode = mode) }

    fun onDishSelect(slot: MenuSlot, dish: Dish?) = form.update { current ->
        current.copy(selections = dish?.let { current.selections + (slot to it) } ?: (current.selections - slot))
    }

    fun onRandomDish(slot: MenuSlot) {
        val state = uiState.value
        randomDish(state.optionsFor(slot), state.selectionFor(slot))?.let { onDishSelect(slot, it) }
    }

    fun onSave() {
        val state = uiState.value
        if (!state.canSave) return
        val menu = state.toMenu() ?: return
        saveDay { saveMealDay(date, menu) }
    }

    override suspend fun clearDay() = clearMealDay(date)

    private fun fillForm(day: MealDay?) = form.update { current ->
        when (val menu = day?.menu) {
            null -> current.copy(isLoading = false)
            else -> current.copy(isLoading = false, hasSavedDay = true, mode = menu.mode(), selections = menu.slots)
        }
    }

    private fun DailyMenu.mode(): MenuMode = when (this) {
        is DailyMenu.Courses -> MenuMode.COURSES
        is DailyMenu.Single -> MenuMode.SINGLE
    }
}
