package com.gabrieltagama.menuplanner.feature.calendar.shopping

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.model.ShoppingItem
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.BuildShoppingListUseCase
import com.gabrieltagama.menuplanner.feature.calendar.navigation.ShoppingListRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Shopping list of the month carried by [ShoppingListRoute]: builds it when the screen opens with
 * lunches and dinners, and rebuilds it whenever a meal is toggled. At least one meal stays selected.
 */
data class ShoppingListUiState(
    val month: YearMonth,
    val isLoading: Boolean = true,
    val items: List<ShoppingItem> = emptyList(),
    val selectedMeals: Set<Meal> = Meal.entries.toSet()
) {
    val canShare: Boolean get() = !isLoading && items.isNotEmpty()
}

@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val buildShoppingList: BuildShoppingListUseCase
) : ViewModel() {

    private val month = savedStateHandle.toRoute<ShoppingListRoute>().yearMonth
    private val _uiState = MutableStateFlow(ShoppingListUiState(month = month))
    val uiState: StateFlow<ShoppingListUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun onMealToggle(meal: Meal) {
        val selected = _uiState.value.selectedMeals
        if (selected == setOf(meal)) return
        _uiState.update { it.copy(selectedMeals = if (meal in selected) selected - meal else selected + meal) }
        load()
    }

    private fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true) }
        loadJob = viewModelScope.launch {
            val items = buildShoppingList(month, _uiState.value.selectedMeals)
            _uiState.update { it.copy(isLoading = false, items = items) }
        }
    }
}
