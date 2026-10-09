package com.gabrieltagama.menuplanner.core.data.database.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Relation of a dish with its ingredient lines. Ingredients are sorted by position in the mapper.
 */
internal data class DishWithIngredients(
    @Embedded val dish: DishEntity,
    @Relation(parentColumn = "id", entityColumn = "dish_id")
    val ingredients: List<IngredientEntity>
)
