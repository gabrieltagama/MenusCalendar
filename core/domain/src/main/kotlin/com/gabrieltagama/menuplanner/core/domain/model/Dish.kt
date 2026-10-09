package com.gabrieltagama.menuplanner.core.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Aggregate root for a dish of the recipe book. The id is a UUID so dishes can be merged
 * across devices when a shared JSON is imported; updatedAt decides which version wins.
 */
data class Dish(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val ingredients: List<Ingredient> = emptyList(),
    val preparation: String = "",
    val type: DishType,
    val heaviness: Heaviness,
    val updatedAt: Instant = Instant.now()
)
