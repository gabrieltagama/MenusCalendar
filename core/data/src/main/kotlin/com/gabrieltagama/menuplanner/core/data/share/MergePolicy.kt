package com.gabrieltagama.menuplanner.core.data.share

import com.gabrieltagama.menuplanner.core.data.database.entity.MealDayEntity
import com.gabrieltagama.menuplanner.core.data.database.entity.dishIds

/**
 * Last-writer-wins merge rule: an item unknown locally is added, a known item is updated only
 * when the incoming updatedAt is strictly newer, anything else is skipped. A meal day is
 * importable only when it has a valid shape (single, or starter + main) and every referenced
 * dish is known after the dishes have been merged.
 */
internal enum class MergeDecision { ADD, UPDATE, SKIP }

internal object MergePolicy {

    fun decide(localUpdatedAt: Long?, incomingUpdatedAt: Long): MergeDecision = when {
        localUpdatedAt == null -> MergeDecision.ADD
        incomingUpdatedAt > localUpdatedAt -> MergeDecision.UPDATE
        else -> MergeDecision.SKIP
    }

    fun isImportable(day: MealDayEntity, knownDishIds: Set<String>): Boolean =
        hasValidShape(day) && knownDishIds.containsAll(day.dishIds)

    private fun hasValidShape(day: MealDayEntity): Boolean =
        day.singleId != null || (day.starterId != null && day.mainId != null)
}

internal data class MergeCount(val added: Int, val updated: Int, val skipped: Int) {
    companion object {
        fun of(decisions: List<MergeDecision>): MergeCount = MergeCount(
            added = decisions.count { it == MergeDecision.ADD },
            updated = decisions.count { it == MergeDecision.UPDATE },
            skipped = decisions.count { it == MergeDecision.SKIP }
        )
    }
}
