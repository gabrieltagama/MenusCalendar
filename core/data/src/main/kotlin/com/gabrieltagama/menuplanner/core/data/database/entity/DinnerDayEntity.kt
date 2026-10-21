package com.gabrieltagama.menuplanner.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Row of the `dinner_days` table, keyed by epoch day, with the single dish of that dinner. The
 * reference is RESTRICT so a dish planned for dinner cannot be deleted.
 */
@Entity(
    tableName = "dinner_days",
    foreignKeys = [
        ForeignKey(entity = DishEntity::class, parentColumns = ["id"], childColumns = ["dish_id"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("dish_id")]
)
internal data class DinnerDayEntity(
    @PrimaryKey override val date: Long,
    @ColumnInfo(name = "dish_id") val dishId: String,
    @ColumnInfo(name = "updated_at") override val updatedAt: Long
) : DayPlanRow
