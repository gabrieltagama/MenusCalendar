package com.gabrieltagama.menuplanner.feature.dishes.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
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
 * Recipe book table: observes every dish and applies search, type and heaviness filters through
 * [DishFilter]. The state carries the applied query; the raw query is also exposed on its own
 * synchronous StateFlow so the text field never lags behind typing.
 */
data class DishListUiState(
    val isLoading: Boolean = true,
    val hasDishes: Boolean = false,
    val query: String = "",
    val selectedType: DishType? = null,
    val selectedHeaviness: Heaviness? = null,
    val dishes: List<Dish> = emptyList()
)

@HiltViewModel
class DishListViewModel @Inject constructor(observeDishes: ObserveDishesUseCase) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val selectedType = MutableStateFlow<DishType?>(null)
    private val selectedHeaviness = MutableStateFlow<Heaviness?>(null)

    val uiState: StateFlow<DishListUiState> =
        combine(observeDishes(), _query, selectedType, selectedHeaviness) { dishes, query, type, heaviness ->
            DishListUiState(
                isLoading = false,
                hasDishes = dishes.isNotEmpty(),
                query = query,
                selectedType = type,
                selectedHeaviness = heaviness,
                dishes = DishFilter.apply(dishes, DishFilterCriteria(query, type, heaviness))
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DishListUiState())

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun onTypeSelect(type: DishType?) {
        selectedType.value = type
    }

    fun onHeavinessSelect(heaviness: Heaviness?) {
        selectedHeaviness.value = heaviness
    }
}
