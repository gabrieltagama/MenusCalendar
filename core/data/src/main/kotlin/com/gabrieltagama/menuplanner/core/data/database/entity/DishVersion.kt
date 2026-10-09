package com.gabrieltagama.menuplanner.core.data.database.entity

import androidx.room.ColumnInfo

/**
 * Projection of a dish id, type and last update. Used to merge imports with a single query
 * (type validates calendar slots) and as a change signal so the calendar refreshes when a dish
 * is edited.
 */
internal data class DishVersion(
    val id: String,
    val type: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
