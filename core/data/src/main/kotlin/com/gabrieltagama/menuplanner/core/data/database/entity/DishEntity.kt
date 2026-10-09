package com.gabrieltagama.menuplanner.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Row of the `dishes` table. Enums are stored by name. New columns must be added through a
 * Room migration registered in Migrations.ALL.
 */
@Entity(tableName = "dishes", indices = [Index("type")])
internal data class DishEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val preparation: String,
    val type: String,
    val heaviness: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
