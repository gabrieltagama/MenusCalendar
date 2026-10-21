package com.gabrieltagama.menuplanner.feature.dishes.list

import com.gabrieltagama.menuplanner.core.domain.model.DishSort
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.model.Rating

/**
 * Callbacks of the recipe book screen, one per user action, so the stateless DishListScreen can be
 * previewed and tested without a ViewModel.
 */
data class DishListActions(
    val onQueryChange: (String) -> Unit,
    val onTypesChange: (Set<DishType>) -> Unit,
    val onHeavinessChange: (Set<Heaviness>) -> Unit,
    val onRatingsChange: (Set<Rating>) -> Unit,
    val onMealTimesChange: (Set<MealTime>) -> Unit,
    val onOpenDish: (String?) -> Unit,
    val onSortChange: (DishSort) -> Unit = {}
)
