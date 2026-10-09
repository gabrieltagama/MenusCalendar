package com.gabrieltagama.menuplanner.core.data.share

import com.gabrieltagama.menuplanner.core.data.database.entity.MealDayEntity
import com.gabrieltagama.menuplanner.core.domain.model.Dish

/**
 * Validated content of an imported document, ready to be merged into the database.
 */
internal data class ImportContent(
    val dishes: List<Dish>,
    val mealDays: List<MealDayEntity>
)
