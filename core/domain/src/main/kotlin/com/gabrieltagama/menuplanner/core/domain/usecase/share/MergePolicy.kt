package com.gabrieltagama.menuplanner.core.domain.usecase.share

import java.time.Instant

/**
 * Last-writer-wins rule of the import: an item unknown locally is added, a known item is updated
 * only when the incoming updatedAt is strictly newer (compared in milliseconds, the stored
 * precision), anything else is skipped. MergeCount adds up the decisions of a merge.
 */
internal enum class MergeDecision { ADD, UPDATE, SKIP }

internal object MergePolicy {

    fun decide(local: Instant?, incoming: Instant): MergeDecision = when {
        local == null -> MergeDecision.ADD
        incoming.toEpochMilli() > local.toEpochMilli() -> MergeDecision.UPDATE
        else -> MergeDecision.SKIP
    }
}

internal data class MergeCount(val added: Int, val updated: Int, val skipped: Int) {

    operator fun plus(other: MergeCount): MergeCount =
        MergeCount(added + other.added, updated + other.updated, skipped + other.skipped)

    companion object {
        fun of(decisions: List<MergeDecision>, invalid: Int = 0): MergeCount = MergeCount(
            added = decisions.count { it == MergeDecision.ADD },
            updated = decisions.count { it == MergeDecision.UPDATE },
            skipped = decisions.count { it == MergeDecision.SKIP } + invalid
        )
    }
}
