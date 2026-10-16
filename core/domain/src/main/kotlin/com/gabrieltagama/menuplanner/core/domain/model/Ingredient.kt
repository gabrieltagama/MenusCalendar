package com.gabrieltagama.menuplanner.core.domain.model

/**
 * Value object: an ingredient line of a dish (name + quantity + unit). A line is valid when it has
 * a name and a finite, non-negative quantity.
 */
data class Ingredient(
    val name: String,
    val quantity: Double,
    val unit: MeasureUnit
) {
    val isValid: Boolean get() = name.isNotBlank() && quantity.isFinite() && quantity >= 0.0
}
