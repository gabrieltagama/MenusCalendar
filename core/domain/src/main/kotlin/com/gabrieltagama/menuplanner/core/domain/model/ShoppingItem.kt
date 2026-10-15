package com.gabrieltagama.menuplanner.core.domain.model

/**
 * Value object: one line of the monthly shopping list, a product with its total quantity in
 * [unit] (grams, millilitres or units, or kilograms and litres for large amounts).
 */
data class ShoppingItem(
    val name: String,
    val quantity: Double,
    val unit: MeasureUnit
)
