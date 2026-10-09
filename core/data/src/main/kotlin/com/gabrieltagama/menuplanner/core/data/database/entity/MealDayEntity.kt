package com.gabrieltagama.menuplanner.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Row of the `meal_days` table, keyed by epoch day. Either single_id or starter_id + main_id
 * is set; every dish reference is RESTRICT so a planned dish cannot be deleted. dishIds is an
 * extension (not a member) so Room does not treat it as a column.
 */
@Entity(
    tableName = "meal_days",
    foreignKeys = [
        ForeignKey(entity = DishEntity::class, parentColumns = ["id"], childColumns = ["starter_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = DishEntity::class, parentColumns = ["id"], childColumns = ["main_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = DishEntity::class, parentColumns = ["id"], childColumns = ["single_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = DishEntity::class, parentColumns = ["id"], childColumns = ["dessert_id"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("starter_id"), Index("main_id"), Index("single_id"), Index("dessert_id")]
)
internal data class MealDayEntity(
    @PrimaryKey val date: Long,
    @ColumnInfo(name = "starter_id") val starterId: String?,
    @ColumnInfo(name = "main_id") val mainId: String?,
    @ColumnInfo(name = "single_id") val singleId: String?,
    @ColumnInfo(name = "dessert_id") val dessertId: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

internal val MealDayEntity.dishIds: List<String>
    get() = listOfNotNull(starterId, mainId, singleId, dessertId)
