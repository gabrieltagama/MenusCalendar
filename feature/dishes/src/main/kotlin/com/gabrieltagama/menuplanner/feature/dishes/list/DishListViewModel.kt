package com.gabrieltagama.menuplanner.feature.dishes.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.model.Rating
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.ObserveDishesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Recipe book table: observes every dish and applies search, type, heaviness, minimum rating and
 * meal time filters through [DishFilter]. The selected filters live in one DishFilterCriteria
 * (without query); the raw query is exposed on its own synchronous StateFlow so the text field
 * never lags behind typing, and the state carries the applied query.
 */
data class DishListUiState(
    val isLoading: Boolean = true,
    val hasDishes: Boolean = false,
    val query: String = "",
    val selectedType: DishType? = null,
    val selectedHeaviness: Heaviness? = null,
    val selectedMinRating: Rating? = null,
    val selectedMealTime: MealTime? = null,
    val dishes: List<Dish> = emptyList()
)

@HiltViewModel
class DishListViewModel @Inject constructor(observeDishes: ObserveDishesUseCase) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val filters = MutableStateFlow(DishFilterCriteria())

    val uiState: StateFlow<DishListUiState> =
        combine(observeDishes(), _query, filters) { dishes, query, criteria ->
            DishListUiState(
                isLoading = false,
                hasDishes = dishes.isNotEmpty(),
                query = query,
                selectedType = criteria.type,
                selectedHeaviness = criteria.heaviness,
                selectedMinRating = criteria.minRating,
                selectedMealTime = criteria.mealTime,
                dishes = DishFilter.apply(dishes, criteria.copy(query = query))
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DishListUiState())

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun onTypeSelect(type: DishType?) = filters.update { it.copy(type = type) }

    fun onHeavinessSelect(heaviness: Heaviness?) = filters.update { it.copy(heaviness = heaviness) }

    fun onMinRatingSelect(minRating: Rating?) = filters.update { it.copy(minRating = minRating) }

    fun onMealTimeSelect(mealTime: MealTime?) = filters.update { it.copy(mealTime = mealTime) }
}
