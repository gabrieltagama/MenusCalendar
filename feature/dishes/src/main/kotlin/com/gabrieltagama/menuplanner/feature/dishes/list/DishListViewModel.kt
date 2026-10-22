package com.gabrieltagama.menuplanner.feature.dishes.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrieltagama.menuplanner.core.domain.common.DefaultDispatcher
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishSort
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.DishUsage
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.model.Rating
import com.gabrieltagama.menuplanner.core.domain.query.DishCatalogQueries
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.ObserveDishUsageUseCase
import com.gabrieltagama.menuplanner.core.ui.state.stateWhileSubscribed
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Recipe book table: observes every dish and applies search and the multiple-selection type,
 * heaviness, rating and meal time filters through [DishFilter]. The selected filters live in one
 * DishFilterCriteria (without query); the raw query is exposed on its own synchronous StateFlow
 * so the text field never lags behind typing, while filtering waits for a short pause in typing
 * and runs off the main thread on a search index rebuilt only when the recipe book changes. The
 * filtered dishes are then ordered by the chosen DishSort, using when each dish was last served.
 */
data class DishListUiState(
    val isLoading: Boolean = true,
    val hasDishes: Boolean = false,
    val query: String = "",
    val selectedTypes: Set<DishType> = emptySet(),
    val selectedHeaviness: Set<Heaviness> = emptySet(),
    val selectedRatings: Set<Rating> = emptySet(),
    val selectedMealTimes: Set<MealTime> = emptySet(),
    val sort: DishSort = DishSort.NAME,
    val usage: DishUsage = DishUsage.NONE,
    val dishes: List<Dish> = emptyList()
)

private data class Listing(val criteria: DishFilterCriteria, val sort: DishSort)

@HiltViewModel
class DishListViewModel @Inject constructor(
    dishCatalog: DishCatalogQueries,
    observeDishUsage: ObserveDishUsageUseCase,
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val filters = MutableStateFlow(DishFilterCriteria())
    private val sort = MutableStateFlow(DishSort.NAME)
    private val listing = combine(filters, sort, ::Listing)

    val uiState: StateFlow<DishListUiState> =
        combine(dishCatalog.observeDishes().map(DishFilter::index), debouncedQuery(), listing, observeDishUsage()) { index, query, (criteria, order), usage ->
            DishListUiState(
                isLoading = false,
                hasDishes = index.isNotEmpty(),
                query = query,
                selectedTypes = criteria.types,
                selectedHeaviness = criteria.heaviness,
                selectedRatings = criteria.ratings,
                selectedMealTimes = criteria.mealTimes,
                sort = order,
                usage = usage,
                dishes = DishFilter.filter(index, criteria.copy(query = query)).let { if (order == DishSort.NAME) it else order.sorted(it, usage) }
            )
        }
            .flowOn(defaultDispatcher)
            .stateWhileSubscribed(viewModelScope, DishListUiState())

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

    fun onSortChange(target: DishSort) {
        sort.value = target
    }

    private companion object {
        const val QUERY_DEBOUNCE_MILLIS = 250L
    }
}
