package com.gabrieltagama.menuplanner.core.domain.model

import com.gabrieltagama.menuplanner.core.domain.model.measure.IngredientKey
import com.gabrieltagama.menuplanner.core.domain.model.measure.IngredientStandardizer
import com.gabrieltagama.menuplanner.core.domain.text.spanishCollator

/**
 * Value object: the shopping list built from the ingredient lines of the planned dishes. Every line
 * is converted to its standard unit and grouped by IngredientKey (ignoring case, accents and
 * plurals), so each product appears once with its total; the most used spelling names it. Grams and
 * millilitres are shown in kilograms and litres from METRIC_FACTOR upwards. Items are sorted
 * alphabetically in Spanish order.
 */
@JvmInline
value class ShoppingList private constructor(val items: List<ShoppingItem>) {

    companion object {
        fun of(lines: List<Ingredient>): ShoppingList = ShoppingList(
            lines.filter { it.name.isNotBlank() }
                .let(IngredientStandardizer::standardizeLines)
                .groupBy { IngredientKey.of(it.name) }
                .map { (_, productLines) -> productLines.toShoppingItem() }
                .sortedWith(compareBy(spanishCollator()) { it.name })
        )
    }
}

private val whitespace = Regex("\\s+")

private fun List<Ingredient>.toShoppingItem(): ShoppingItem {
    val total = sumOf { it.quantity }
    val unit = first().unit
    val largerUnit = unit.largerUnit()
    return when {
        largerUnit != null && total >= MeasureUnit.METRIC_FACTOR -> ShoppingItem(name = displayName(), quantity = total / MeasureUnit.METRIC_FACTOR, unit = largerUnit)
        else -> ShoppingItem(name = displayName(), quantity = total, unit = unit)
    }
}

private fun List<Ingredient>.displayName(): String =
    map { it.name.trim().replace(whitespace, " ") }
        .groupingBy { it }
        .eachCount()
        .maxBy { it.value }
        .key

private fun MeasureUnit.largerUnit(): MeasureUnit? = when (this) {
    MeasureUnit.GRAM -> MeasureUnit.KILOGRAM
    MeasureUnit.MILLILITER -> MeasureUnit.LITER
    else -> null
}
