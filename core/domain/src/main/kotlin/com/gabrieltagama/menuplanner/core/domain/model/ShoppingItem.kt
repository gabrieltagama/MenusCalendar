package com.gabrieltagama.menuplanner.core.domain.model

import com.gabrieltagama.menuplanner.core.domain.model.measure.IngredientKey

/**
 * Value object: one line of the monthly shopping list, a product with its total quantity in
 * [unit] (grams, millilitres or units, or kilograms and litres for large amounts). key identifies
 * the product whatever its spelling (IngredientKey), so it can be ticked as bought.
 */
data class ShoppingItem(
    val name: String,
    val quantity: Double,
    val unit: MeasureUnit
) {
    val key: String get() = IngredientKey.of(name).value
}
