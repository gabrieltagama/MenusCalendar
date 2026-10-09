package com.gabrieltagama.menuplanner.core.data.share

import com.gabrieltagama.menuplanner.core.data.database.entity.MealDayEntity
import com.gabrieltagama.menuplanner.core.domain.model.DishType

/**
 * Last-writer-wins merge rule: an item unknown locally is added, a known item is updated only
 * when the incoming updatedAt is strictly newer, anything else is skipped. A meal day is
 * importable only when it has exactly one valid shape (single, or starter + main, never both)
 * and every referenced dish is known after the dishes have been merged and sits in the slot
 * matching its type, the same rule SaveMealDayUseCase applies.
 */
internal enum class MergeDecision { ADD, UPDATE, SKIP }

internal object MergePolicy {

    fun decide(localUpdatedAt: Long?, incomingUpdatedAt: Long): MergeDecision = when {
        localUpdatedAt == null -> MergeDecision.ADD
        incomingUpdatedAt > localUpdatedAt -> MergeDecision.UPDATE
        else -> MergeDecision.SKIP
    }

    fun isImportable(day: MealDayEntity, knownDishTypes: Map<String, DishType>): Boolean =
        hasValidShape(day) && day.slots().all { (dishId, expected) -> knownDishTypes[dishId] == expected }

    private fun hasValidShape(day: MealDayEntity): Boolean {
        val isSingle = day.singleId != null && day.starterId == null && day.mainId == null
        val isCourses = day.singleId == null && day.starterId != null && day.mainId != null
        return isSingle || isCourses
    }

    private fun MealDayEntity.slots(): List<Pair<String, DishType>> = listOfNotNull(
        starterId?.let { it to DishType.STARTER },
        mainId?.let { it to DishType.MAIN },
        singleId?.let { it to DishType.SINGLE },
        dessertId?.let { it to DishType.DESSERT }
    )
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
