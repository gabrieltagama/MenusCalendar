package com.gabrieltagama.menuplanner.core.domain.model

/**
 * Value object: an ingredient line of a dish (name + quantity + unit).
 */
data class Ingredient(
    val name: String,
    val quantity: Double,
    val unit: MeasureUnit
)
