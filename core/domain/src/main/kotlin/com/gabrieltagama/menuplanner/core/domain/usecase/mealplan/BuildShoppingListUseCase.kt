package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.ShoppingItem
import com.gabrieltagama.menuplanner.core.domain.model.measure.IngredientKey
import com.gabrieltagama.menuplanner.core.domain.model.measure.IngredientStandardizer
import com.gabrieltagama.menuplanner.core.domain.repository.MealPlanRepository
import java.text.Collator
import java.time.YearMonth
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Builds the shopping list of a month from the starters, mains and single dishes planned in it;
 * desserts are left out. A dish counts once per day it is planned. Every line is converted to its
 * standard unit and grouped by IngredientKey (ignoring case, accents and plurals), so each product
 * appears once with its total; the most used spelling names it. Grams and millilitres are shown
 * in kilograms and litres from 1000 upwards. The result is sorted alphabetically in Spanish order.
 */
class BuildShoppingListUseCase @Inject constructor(private val repository: MealPlanRepository) {

    suspend operator fun invoke(month: YearMonth): List<ShoppingItem> =
        repository.observeMonth(month).first()
            .flatMap { it.menu.mainDishes }
            .flatMap { it.ingredients }
            .filter { it.name.isNotBlank() }
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
