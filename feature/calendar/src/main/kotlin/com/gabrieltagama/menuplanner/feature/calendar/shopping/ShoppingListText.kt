package com.gabrieltagama.menuplanner.feature.calendar.shopping

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gabrieltagama.menuplanner.core.domain.model.ShoppingItem
import com.gabrieltagama.menuplanner.core.ui.text.label
import com.gabrieltagama.menuplanner.feature.calendar.R
import com.gabrieltagama.menuplanner.feature.calendar.common.inlineText
import com.gabrieltagama.menuplanner.feature.calendar.common.spanishLocale
import java.text.NumberFormat

/**
 * Spanish texts of the shopping list: the amount of a line ("1,5 kg", "al gusto") and the plain
 * text shared with other apps, one "• name: amount" line per ingredient under a month header.
 */
internal fun Double.quantityText(): String =
    NumberFormat.getNumberInstance(spanishLocale).apply { maximumFractionDigits = 2 }.format(this)

@Composable
internal fun ShoppingItem.amountText(): String =
    quantity?.let { "${it.quantityText()} ${unit.label()}" } ?: unit.label()

@Composable
internal fun ShoppingListUiState.shareText(): String {
    val header = stringResource(R.string.shopping_list_share_header, month.inlineText())
    val lines = items.map { "• ${it.name}: ${it.amountText()}" }
    return (listOf(header, "") + lines).joinToString("\n")
}
