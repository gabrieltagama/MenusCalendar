package com.gabrieltagama.menuplanner.feature.calendar.shopping

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.model.ShoppingItem
import com.gabrieltagama.menuplanner.core.ui.text.label
import com.gabrieltagama.menuplanner.feature.calendar.R
import com.gabrieltagama.menuplanner.feature.calendar.common.spanishLocale
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Spanish texts of the shopping list: the amount of a line ("1,5 kg", "3 ud."), a date of the
 * range ("15 oct 2026"), the range itself ("15 oct 2026 – 31 oct 2026"), the meals it covers
 * ("solo comidas", "solo cenas", "comidas y cenas") and the plain text shared with other apps,
 * one "• name: amount" line per ingredient under a header with the range.
 */
private val shortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", spanishLocale)

internal fun Double.quantityText(): String =
    NumberFormat.getNumberInstance(spanishLocale).apply { maximumFractionDigits = 2 }.format(this)

internal fun LocalDate.shortText(): String = format(shortDateFormatter)

internal fun ShoppingListUiState.rangeText(): String = "${from.shortText()} – ${to.shortText()}"

@Composable
internal fun ShoppingItem.amountText(): String =
    "${quantity.quantityText()} ${unit.label()}"

@Composable
internal fun ShoppingListUiState.mealsText(): String = stringResource(
    when (selectedMeals) {
        setOf(Meal.LUNCH) -> R.string.shopping_list_meals_lunch
        setOf(Meal.DINNER) -> R.string.shopping_list_meals_dinner
        else -> R.string.shopping_list_meals_all
    }
)

@Composable
internal fun ShoppingListUiState.shareText(): String {
    val header = stringResource(R.string.shopping_list_share_header, rangeText(), mealsText())
    val lines = items.map { "• ${it.name}: ${it.amountText()}" }
    return (listOf(header, "") + lines).joinToString("\n")
}
