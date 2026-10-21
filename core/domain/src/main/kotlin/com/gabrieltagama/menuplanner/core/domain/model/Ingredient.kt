package com.gabrieltagama.menuplanner.core.domain.model

/**
 * Value object: an ingredient line of a dish (name + quantity + unit). A line is valid when its name
 * and its quantity meet the rules of IngredientName and Quantity.
 */
data class Ingredient(
    val name: String,
    val quantity: Double,
    val unit: MeasureUnit
) {
    val isValid: Boolean get() = IngredientName.isValid(name) && Quantity.isValid(quantity)
}
