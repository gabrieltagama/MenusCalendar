package com.gabrieltagama.menuplanner.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.model.Rating
import java.time.Instant

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
    val type: DishType,
    val heaviness: Heaviness,
    @ColumnInfo(defaultValue = "THREE") val rating: Rating,
    @ColumnInfo(name = "meal_time", defaultValue = "ANY") val mealTime: MealTime,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant
)
