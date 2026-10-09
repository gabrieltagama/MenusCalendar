package com.gabrieltagama.menuplanner.core.domain.model

/**
 * Value object: one line of the monthly shopping list, an ingredient with its total quantity in
 * [unit]. The quantity is null for units that are not measured (pinch, to taste).
 */
data class ShoppingItem(
    val name: String,
    val quantity: Double?,
    val unit: MeasureUnit
)
