package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.ShoppingItem
import com.gabrieltagama.menuplanner.core.domain.model.measure.IngredientKey
import com.gabrieltagama.menuplanner.core.domain.model.measure.IngredientStandardizer
import com.gabrieltagama.menuplanner.core.domain.query.ShoppingListQueries
import java.text.Collator
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

/**
 * Observes the shopping list of a month or of a date range (both ends included, it may span several
 * months) from the starters, mains and single dishes planned for lunch and the dishes planned for
 * dinner, limited to the requested meals (both by default), and emits it again whenever the plan or
 * a planned dish changes. It reads only ingredient lines (ShoppingListQueries), never whole dishes.
 * A dish counts once per meal it is planned in. Every line is converted to its standard unit and
 * grouped by IngredientKey (ignoring case, accents and plurals), so each product appears once with
 * its total; the most used spelling names it. Grams and millilitres are shown in kilograms and
 * litres from 1000 upwards. The result is sorted alphabetically in Spanish order.
 */
class ObserveShoppingListUseCase @Inject constructor(private val queries: ShoppingListQueries) {

    operator fun invoke(month: YearMonth, meals: Set<Meal> = Meal.entries.toSet()): Flow<List<ShoppingItem>> =
        invoke(month.atDay(1)..month.atEndOfMonth(), meals)

    operator fun invoke(dates: ClosedRange<LocalDate>, meals: Set<Meal> = Meal.entries.toSet()): Flow<List<ShoppingItem>> {
        val lineFlows = Meal.entries.filter { it in meals }.map { queries.observeIngredientLines(dates, it) }
        if (lineFlows.isEmpty()) return flowOf(emptyList())
        return combine(lineFlows) { lines -> lines.toList().flatten().toShoppingList() }
    }

    private fun List<Ingredient>.toShoppingList(): List<ShoppingItem> =
        filter { it.name.isNotBlank() }
            .let(IngredientStandardizer::standardizeLines)
            .groupBy { IngredientKey.of(it.name) }
            .map { (_, lines) -> lines.toShoppingItem() }
            .sortedWith(compareBy(spanishCollator()) { it.name })
}

private const val METRIC_FACTOR = 1000.0

private val spanishLocale: Locale = Locale.forLanguageTag("es-ES")

private val whitespace = Regex("\\s+")

private fun spanishCollator(): Collator = Collator.getInstance(spanishLocale).apply { strength = Collator.PRIMARY }

private fun List<Ingredient>.toShoppingItem(): ShoppingItem {
    val total = sumOf { it.quantity }
    val unit = first().unit
    val largerUnit = unit.largerUnit()
    return when {
        largerUnit != null && total >= METRIC_FACTOR -> ShoppingItem(name = displayName(), quantity = total / METRIC_FACTOR, unit = largerUnit)
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
