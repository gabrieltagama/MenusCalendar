package com.gabrieltagama.menuplanner.feature.calendar.shopping

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.model.ShoppingFilter
import com.gabrieltagama.menuplanner.core.domain.model.ShoppingItem
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ClearExpiredShoppingChecksUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ClearShoppingChecksUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ObserveShoppingChecksUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ObserveShoppingListUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.SetShoppingItemCheckedUseCase
import com.gabrieltagama.menuplanner.feature.calendar.navigation.ShoppingListRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Shopping list opened from the month carried by [ShoppingListRoute]. It covers a date range,
 * from today (or the first day, when the month is not the current one) to the end of the month
 * by default, and lunches and dinners. The list is observed, so it is rebuilt (off the main thread)
 * whenever the range, the meals, the plan or a planned dish change; the range never ends before it starts and at least one meal stays selected. Products can be
 * ticked as bought; ticks belong to the current range and meals, so changing them shows that
 * filter's own ticks and going back restores them. Ticks are kept until unticked, cleared or the
 * range is over (cleared on opening). Bought products move to the end of the list and only the
 * pending ones are shared.
 */
data class ShoppingListUiState(
    val from: LocalDate,
    val to: LocalDate,
    val isLoading: Boolean = true,
    val items: List<ShoppingItem> = emptyList(),
    val selectedMeals: Set<Meal> = Meal.entries.toSet(),
    val checked: Set<String> = emptySet()
) {
    val filter: ShoppingFilter get() = ShoppingFilter(from = from, to = to, meals = selectedMeals)

    val pendingItems: List<ShoppingItem> get() = items.filterNot(::isChecked)

    val sortedItems: List<ShoppingItem> get() = items.sortedBy(::isChecked)

    val hasChecked: Boolean get() = items.any(::isChecked)

    val canShare: Boolean get() = !isLoading && pendingItems.isNotEmpty()

    fun isChecked(item: ShoppingItem): Boolean = item.key in checked
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    clock: Clock,
    observeShoppingChecks: ObserveShoppingChecksUseCase,
    observeShoppingList: ObserveShoppingListUseCase,
    clearExpiredShoppingChecks: ClearExpiredShoppingChecksUseCase,
    private val setShoppingItemChecked: SetShoppingItemCheckedUseCase,
    private val clearShoppingChecks: ClearShoppingChecksUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        savedStateHandle.toRoute<ShoppingListRoute>().yearMonth.initialState(LocalDate.now(clock))
    )
    val uiState: StateFlow<ShoppingListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            filters()
                .onEach { _uiState.update { it.copy(isLoading = true) } }
                .flatMapLatest { filter -> observeShoppingList(filter.dates, filter.meals).flowOn(Dispatchers.Default) }
                .collect { items -> _uiState.update { it.copy(isLoading = false, items = items) } }
        }
        viewModelScope.launch {
            clearExpiredShoppingChecks()
            filters()
                .flatMapLatest { filter -> observeShoppingChecks(filter) }
                .collect { keys -> _uiState.update { it.copy(checked = keys) } }
        }
    }

    fun onItemCheck(item: ShoppingItem, isChecked: Boolean) {
        val filter = _uiState.value.filter
        viewModelScope.launch { setShoppingItemChecked(filter, item, isChecked) }
    }

    fun onClearChecks() {
        val filter = _uiState.value.filter
        viewModelScope.launch { clearShoppingChecks(filter) }
    }

    fun onMealToggle(meal: Meal) {
        val selected = _uiState.value.selectedMeals
        if (selected == setOf(meal)) return
        _uiState.update { it.copy(selectedMeals = if (meal in selected) selected - meal else selected + meal) }
    }

    fun onFromChange(date: LocalDate) {
        _uiState.update { it.copy(from = date, to = maxOf(it.to, date)) }
    }

    fun onToChange(date: LocalDate) {
        _uiState.update { it.copy(from = minOf(it.from, date), to = date) }
    }

    private fun filters(): Flow<ShoppingFilter> = _uiState.map(ShoppingListUiState::filter).distinctUntilChanged()
}

private fun YearMonth.initialState(today: LocalDate): ShoppingListUiState = ShoppingListUiState(
    from = if (YearMonth.from(today) == this) today else atDay(1),
    to = atEndOfMonth()
)
