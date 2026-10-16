package com.gabrieltagama.menuplanner.feature.calendar.shopping

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.model.ShoppingItem
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
 * ticked as bought; ticks are kept until unticked or cleared, bought products move to the end of
 * the list and only the pending ones are shared.
 */
data class ShoppingListUiState(
    val from: LocalDate,
    val to: LocalDate,
    val isLoading: Boolean = true,
    val items: List<ShoppingItem> = emptyList(),
    val selectedMeals: Set<Meal> = Meal.entries.toSet(),
    val checked: Set<String> = emptySet()
) {
    val pendingItems: List<ShoppingItem> get() = items.filterNot(::isChecked)

    val sortedItems: List<ShoppingItem> get() = items.sortedBy(::isChecked)

    val hasChecked: Boolean get() = items.any(::isChecked)

    val canShare: Boolean get() = !isLoading && pendingItems.isNotEmpty()

    fun isChecked(item: ShoppingItem): Boolean = item.key in checked
}

private data class ShoppingCriteria(val dates: ClosedRange<LocalDate>, val meals: Set<Meal>)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    clock: Clock,
    observeShoppingChecks: ObserveShoppingChecksUseCase,
    observeShoppingList: ObserveShoppingListUseCase,
    private val setShoppingItemChecked: SetShoppingItemCheckedUseCase,
    private val clearShoppingChecks: ClearShoppingChecksUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        savedStateHandle.toRoute<ShoppingListRoute>().yearMonth.initialState(LocalDate.now(clock))
    )
    val uiState: StateFlow<ShoppingListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.map { ShoppingCriteria(it.from..it.to, it.selectedMeals) }
                .distinctUntilChanged()
                .onEach { _uiState.update { it.copy(isLoading = true) } }
                .flatMapLatest { criteria -> observeShoppingList(criteria.dates, criteria.meals).flowOn(Dispatchers.Default) }
                .collect { items -> _uiState.update { it.copy(isLoading = false, items = items) } }
        }
        viewModelScope.launch { observeShoppingChecks().collect { keys -> _uiState.update { it.copy(checked = keys) } } }
    }

    fun onItemCheck(item: ShoppingItem, isChecked: Boolean) {
        viewModelScope.launch { setShoppingItemChecked(item, isChecked) }
    }

    fun onClearChecks() {
        viewModelScope.launch { clearShoppingChecks() }
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
}

private fun YearMonth.initialState(today: LocalDate): ShoppingListUiState = ShoppingListUiState(
    from = if (YearMonth.from(today) == this) today else atDay(1),
    to = atEndOfMonth()
)
