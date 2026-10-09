package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.ShoppingItem
import com.gabrieltagama.menuplanner.core.domain.repository.MealPlanRepository
import java.text.Collator
import java.time.YearMonth
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Builds the shopping list of a month from the starters, mains and single dishes planned in it;
 * desserts are left out. A dish counts once per day it is planned. Ingredients are grouped by
 * name ignoring case and extra spaces; grams with kilograms and millilitres with litres are added
 * together and shown in the larger unit from 1000 upwards. Pinch and to-taste lines appear once,
 * without quantity. The result is sorted alphabetically in Spanish order.
 */
class BuildShoppingListUseCase @Inject constructor(private val repository: MealPlanRepository) {

    suspend operator fun invoke(month: YearMonth): List<ShoppingItem> =
        repository.observeMonth(month).first()
            .flatMap { it.menu.mainDishes }
            .flatMap { it.ingredients }
            .filter { it.name.isNotBlank() }
            .groupBy { ShoppingKey(it.name.normalized(), it.unit.baseUnit()) }
            .map { (key, lines) -> lines.toShoppingItem(key.unit) }
            .sortedWith(compareBy(spanishCollator()) { it.name })

    private data class ShoppingKey(val name: String, val unit: MeasureUnit)
}

private const val METRIC_FACTOR = 1000.0

private val spanishLocale: Locale = Locale.forLanguageTag("es-ES")

private val whitespace = Regex("\\s+")

private fun spanishCollator(): Collator = Collator.getInstance(spanishLocale).apply { strength = Collator.PRIMARY }

private fun String.normalized(): String = trim().replace(whitespace, " ").lowercase(spanishLocale)

private fun List<Ingredient>.toShoppingItem(baseUnit: MeasureUnit): ShoppingItem {
    val name = first().name.trim().replace(whitespace, " ")
    val total = sumOf { it.quantity * it.unit.baseFactor() }
    val largerUnit = baseUnit.largerUnit()
    return when {
        !baseUnit.isMeasured() -> ShoppingItem(name = name, quantity = null, unit = baseUnit)
        largerUnit != null && total >= METRIC_FACTOR -> ShoppingItem(name = name, quantity = total / METRIC_FACTOR, unit = largerUnit)
        else -> ShoppingItem(name = name, quantity = total, unit = baseUnit)
    }
}

private fun MeasureUnit.baseUnit(): MeasureUnit = when (this) {
    MeasureUnit.KILOGRAM -> MeasureUnit.GRAM
    MeasureUnit.LITER -> MeasureUnit.MILLILITER
    else -> this
}

private fun MeasureUnit.baseFactor(): Double = when (this) {
    MeasureUnit.KILOGRAM, MeasureUnit.LITER -> METRIC_FACTOR
    else -> 1.0
}

private fun MeasureUnit.largerUnit(): MeasureUnit? = when (this) {
    MeasureUnit.GRAM -> MeasureUnit.KILOGRAM
    MeasureUnit.MILLILITER -> MeasureUnit.LITER
    else -> null
}

private fun MeasureUnit.isMeasured(): Boolean = this != MeasureUnit.PINCH && this != MeasureUnit.TO_TASTE
