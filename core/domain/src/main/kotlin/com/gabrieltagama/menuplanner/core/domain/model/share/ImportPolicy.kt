package com.gabrieltagama.menuplanner.core.domain.model.share

import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import java.time.Instant

/**
 * Last-writer-wins rule of every import: an item unknown locally is added, a known item is updated
 * only when the incoming updatedAt is strictly newer, anything else is skipped. summaryOf counts the
 * decisions of the dishes and of the planned days (lunches and dinners together).
 */
enum class MergeDecision { ADD, UPDATE, SKIP }

object ImportPolicy {

    fun decide(localUpdatedAt: Instant?, incomingUpdatedAt: Instant): MergeDecision = when {
        localUpdatedAt == null -> MergeDecision.ADD
        incomingUpdatedAt > localUpdatedAt -> MergeDecision.UPDATE
        else -> MergeDecision.SKIP
    }

    fun summaryOf(dishes: List<MergeDecision>, days: List<MergeDecision>): ImportSummary = ImportSummary(
        dishesAdded = dishes.count { it == MergeDecision.ADD },
        dishesUpdated = dishes.count { it == MergeDecision.UPDATE },
        dishesSkipped = dishes.count { it == MergeDecision.SKIP },
        daysAdded = days.count { it == MergeDecision.ADD },
        daysUpdated = days.count { it == MergeDecision.UPDATE },
        daysSkipped = days.count { it == MergeDecision.SKIP }
    )
}
