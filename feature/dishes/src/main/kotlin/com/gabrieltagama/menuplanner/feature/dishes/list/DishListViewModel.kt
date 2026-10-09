package com.gabrieltagama.menuplanner.feature.dishes.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.ObserveDishesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Recipe book list: observes every dish and applies the type filter and the name search.
 * The raw query is exposed on its own synchronous StateFlow so the text field never lags.
 */
data class DishListUiState(
    val isLoading: Boolean = true,
    val hasDishes: Boolean = false,
    val selectedType: DishType? = null,
    val dishes: List<Dish> = emptyList()
)

@HiltViewModel
class DishListViewModel @Inject constructor(observeDishes: ObserveDishesUseCase) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val selectedType = MutableStateFlow<DishType?>(null)

    val uiState: StateFlow<DishListUiState> =
        combine(observeDishes(), _query, selectedType) { dishes, query, type ->
            val normalizedQuery = query.normalizedForSearch()
            DishListUiState(
                isLoading = false,
                hasDishes = dishes.isNotEmpty(),
                selectedType = type,
                dishes = dishes
                    .filter { type == null || it.type == type }
                    .filter { normalizedQuery.isEmpty() || it.name.normalizedForSearch().contains(normalizedQuery) }
                    .sortedBy { it.name.normalizedForSearch() }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DishListUiState())

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun onTypeSelect(type: DishType?) {
        selectedType.value = type
    }
}
