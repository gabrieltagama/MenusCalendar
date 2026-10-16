package com.gabrieltagama.menuplanner.core.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.DishSort
import com.gabrieltagama.menuplanner.core.domain.model.DishUsage
import com.gabrieltagama.menuplanner.core.ui.R

/**
 * Localized labels for the dish orders and for how long ago a dish was last served ("Nunca",
 * "Hoy", "Ayer", "Hace 12 días").
 */
@Composable
fun DishSort.label(): String = stringResource(
    when (this) {
        DishSort.NAME -> R.string.dish_sort_name
        DishSort.LAST_SERVED -> R.string.dish_sort_last_served
        DishSort.RATING -> R.string.dish_sort_rating
        DishSort.HEAVINESS -> R.string.dish_sort_heaviness
    }
)

@Composable
fun DishUsage.lastServedText(id: DishId): String = when (val days = daysSinceServed(id)) {
    null -> stringResource(R.string.last_served_never)
    0L -> stringResource(R.string.last_served_today)
    1L -> stringResource(R.string.last_served_yesterday)
    else -> pluralStringResource(R.plurals.last_served_days_ago, days.toInt(), days.toInt())
}
