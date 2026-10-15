package com.gabrieltagama.menuplanner.core.domain.model.measure

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import kotlin.math.roundToLong

/**
 * Domain service that leaves every ingredient in one standard unit so equal products add up:
 * grams for solids, millilitres for liquids, or units for products that are only ever counted.
 * Kilograms and litres are scaled; a tablespoon is 15 ml or 20 g, a teaspoon 5 ml or 5 g and a
 * cup 240 ml or 200 g; a pinch or "to taste" becomes the catalog amount of the ingredient (10 g or
 * ml when unknown). A product counted in units in some lines and weighed in others is weighed
 * everywhere, using its estimated weight per unit (100 g when unknown). Lines are grouped by
 * IngredientKey across everything given, so a product ends with a single unit in all of it.
 */
object IngredientStandardizer {

    fun standardize(dishes: List<Dish>): List<Dish> {
        val targets = dishes.flatMap { it.ingredients }.targetUnits()
        return dishes.map { dish -> dish.copy(ingredients = dish.ingredients.map { it.toStandard(targets) }) }
    }

    fun standardizeLines(ingredients: List<Ingredient>): List<Ingredient> {
        val targets = ingredients.targetUnits()
        return ingredients.map { it.toStandard(targets) }
    }
}

private const val METRIC_FACTOR = 1000.0
private const val LIQUID_TABLESPOON = 15.0
private const val SOLID_TABLESPOON = 20.0
private const val TEASPOON = 5.0
private const val LIQUID_CUP = 240.0
private const val SOLID_CUP = 200.0
private const val DECIMALS_FACTOR = 100.0

private fun List<Ingredient>.targetUnits(): Map<IngredientKey, MeasureUnit> =
    groupBy { IngredientKey.of(it.name) }.mapValues { (key, lines) -> lines.targetUnit(key) }

private fun List<Ingredient>.targetUnit(key: IngredientKey): MeasureUnit = when {
    all { it.unit == MeasureUnit.UNIT } -> MeasureUnit.UNIT
    else -> substanceOf(key).standardUnit
}

private fun List<Ingredient>.substanceOf(key: IngredientKey): Substance =
    IngredientCatalog.profileOf(key)?.substance
        ?: if (any { it.unit.isVolume }) Substance.LIQUID else Substance.SOLID

private fun Ingredient.toStandard(targets: Map<IngredientKey, MeasureUnit>): Ingredient {
    val key = IngredientKey.of(name)
    val target = targets.getValue(key)
    if (target == MeasureUnit.UNIT) return this
    val profile = IngredientCatalog.profileOf(key)
    val isLiquid = target == MeasureUnit.MILLILITER
    val amount = when (unit) {
        MeasureUnit.GRAM, MeasureUnit.MILLILITER -> quantity
        MeasureUnit.KILOGRAM, MeasureUnit.LITER -> quantity * METRIC_FACTOR
        MeasureUnit.UNIT -> quantity * (profile?.amountPerUnit ?: IngredientCatalog.DEFAULT_AMOUNT_PER_UNIT)
        MeasureUnit.TABLESPOON -> quantity * if (isLiquid) LIQUID_TABLESPOON else SOLID_TABLESPOON
        MeasureUnit.TEASPOON -> quantity * TEASPOON
        MeasureUnit.CUP -> quantity * if (isLiquid) LIQUID_CUP else SOLID_CUP
        MeasureUnit.PINCH, MeasureUnit.TO_TASTE -> quantity.orOne() * (profile?.toTasteAmount ?: IngredientCatalog.DEFAULT_TO_TASTE_AMOUNT)
    }
    return copy(quantity = amount.rounded(), unit = target)
}

private fun Double.orOne(): Double = takeIf { it > 0.0 } ?: 1.0

private fun Double.rounded(): Double = (this * DECIMALS_FACTOR).roundToLong() / DECIMALS_FACTOR
