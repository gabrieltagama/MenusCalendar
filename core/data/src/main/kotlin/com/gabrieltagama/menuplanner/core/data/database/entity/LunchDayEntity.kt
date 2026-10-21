package com.gabrieltagama.menuplanner.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gabrieltagama.menuplanner.core.domain.model.MenuSlot
import java.time.Instant
import java.time.LocalDate

/**
 * Row of the `meal_days` table, keyed by epoch day. Either single_id or starter_id + main_id
 * is set; every dish reference is RESTRICT so a planned dish cannot be deleted. slotIds and dishIds
 * are extensions (not members) so Room does not treat them as columns.
 */
@Entity(
    tableName = "meal_days",
    foreignKeys = [
        ForeignKey(entity = DishEntity::class, parentColumns = ["id"], childColumns = ["starter_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = DishEntity::class, parentColumns = ["id"], childColumns = ["main_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = DishEntity::class, parentColumns = ["id"], childColumns = ["single_id"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("starter_id"), Index("main_id"), Index("single_id")]
)
internal data class LunchDayEntity(
    @PrimaryKey override val date: LocalDate,
    @ColumnInfo(name = "starter_id") val starterId: String?,
    @ColumnInfo(name = "main_id") val mainId: String?,
    @ColumnInfo(name = "single_id") val singleId: String?,
    @ColumnInfo(name = "updated_at") override val updatedAt: Instant
) : DayPlanRow

internal val LunchDayEntity.slotIds: Map<MenuSlot, String>
    get() = listOfNotNull(starterId?.let { MenuSlot.STARTER to it }, mainId?.let { MenuSlot.MAIN to it }, singleId?.let { MenuSlot.SINGLE to it }).toMap()

internal val LunchDayEntity.dishIds: List<String>
    get() = slotIds.values.toList()
