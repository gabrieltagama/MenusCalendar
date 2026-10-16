package com.gabrieltagama.menuplanner.core.data.database.entity

import androidx.room.ColumnInfo

/**
 * Projection of a dish id, type, meal time and last update. Used to merge imports with a single
 * query (type validates calendar slots, type and meal time validate dinners) and to check whether
 * the recipe book is empty.
 */
internal data class DishVersion(
    val id: String,
    val type: String,
    @ColumnInfo(name = "meal_time") val mealTime: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
