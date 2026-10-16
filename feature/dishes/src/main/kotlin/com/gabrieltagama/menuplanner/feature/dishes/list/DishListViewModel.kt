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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Recipe book table: observes every dish and applies search and the multiple-selection type,
 * heaviness, rating and meal time filters through [DishFilter]. The selected filters live in one
 * DishFilterCriteria (without query); the raw query is exposed on its own synchronous StateFlow
 * so the text field never lags behind typing, while filtering waits for a short pause in typing
 * and runs off the main thread on a search index rebuilt only when the recipe book changes.
 */
data class DishListUiState(
    val isLoading: Boolean = true,
    val hasDishes: Boolean = false,
    val query: String = "",
    val selectedTypes: Set<DishType> = emptySet(),
    val selectedHeaviness: Set<Heaviness> = emptySet(),
    val selectedRatings: Set<Rating> = emptySet(),
    val selectedMealTimes: Set<MealTime> = emptySet(),
    val dishes: List<Dish> = emptyList()
)

@HiltViewModel
class DishListViewModel @Inject constructor(observeDishes: ObserveDishesUseCase) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val filters = MutableStateFlow(DishFilterCriteria())

    val uiState: StateFlow<DishListUiState> =
        combine(observeDishes().map(DishFilter::index), debouncedQuery(), filters) { index, query, criteria ->
            DishListUiState(
                isLoading = false,
                hasDishes = index.isNotEmpty(),
                query = query,
                selectedTypes = criteria.types,
                selectedHeaviness = criteria.heaviness,
                selectedRatings = criteria.ratings,
                selectedMealTimes = criteria.mealTimes,
                dishes = DishFilter.apply(index, criteria.copy(query = query))
            )
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DishListUiState())

    fun onQueryChange(value: String) {
        _query.value = value
    }

    @OptIn(FlowPreview::class)
    private fun debouncedQuery(): Flow<String> =
        _query.debounce { if (it.isEmpty()) 0L else QUERY_DEBOUNCE_MILLIS }.distinctUntilChanged()

    fun onTypesChange(types: Set<DishType>) = filters.update { it.copy(types = types) }

    fun onHeavinessChange(heaviness: Set<Heaviness>) = filters.update { it.copy(heaviness = heaviness) }

    fun onRatingsChange(ratings: Set<Rating>) = filters.update { it.copy(ratings = ratings) }

    fun onMealTimesChange(mealTimes: Set<MealTime>) = filters.update { it.copy(mealTimes = mealTimes) }

    private companion object {
        const val QUERY_DEBOUNCE_MILLIS = 250L
    }
}
