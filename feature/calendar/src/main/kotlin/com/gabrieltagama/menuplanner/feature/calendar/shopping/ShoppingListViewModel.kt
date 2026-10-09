package com.gabrieltagama.menuplanner.feature.calendar.shopping

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gabrieltagama.menuplanner.core.domain.model.ShoppingItem
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.BuildShoppingListUseCase
import com.gabrieltagama.menuplanner.feature.calendar.navigation.ShoppingListRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Shopping list of the month carried by [ShoppingListRoute]: builds it once when the screen opens.
 */
data class ShoppingListUiState(
    val month: YearMonth,
    val isLoading: Boolean = true,
    val items: List<ShoppingItem> = emptyList()
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

    init {
        viewModelScope.launch {
            val items = buildShoppingList(month)
            _uiState.update { it.copy(isLoading = false, items = items) }
        }
    }
}
