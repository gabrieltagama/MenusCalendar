package com.gabrieltagama.menuplanner.core.domain.model.share

import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests the last-writer-wins rule of the import and how decisions are counted in the summary.
 */
class ImportPolicyTest {
    private val older = Instant.parse("2026-01-01T00:00:00Z")
    private val newer = Instant.parse("2026-02-01T00:00:00Z")

    @Test
    fun `unknown item is added`() = assertEquals(MergeDecision.ADD, ImportPolicy.decide(localUpdatedAt = null, incomingUpdatedAt = older))

    @Test
    fun `newer incoming item updates`() = assertEquals(MergeDecision.UPDATE, ImportPolicy.decide(localUpdatedAt = older, incomingUpdatedAt = newer))

    @Test
    fun `older incoming item is skipped`() = assertEquals(MergeDecision.SKIP, ImportPolicy.decide(localUpdatedAt = newer, incomingUpdatedAt = older))

    @Test
    fun `incoming item with equal timestamp is skipped`() = assertEquals(MergeDecision.SKIP, ImportPolicy.decide(localUpdatedAt = older, incomingUpdatedAt = older))

    @Test
    fun `summary counts dish and day decisions`() {
        val dishes = listOf(MergeDecision.ADD, MergeDecision.SKIP, MergeDecision.ADD, MergeDecision.UPDATE)
        val days = listOf(MergeDecision.SKIP, MergeDecision.SKIP, MergeDecision.UPDATE)

        assertEquals(ImportSummary(2, 1, 1, 0, 1, 2), ImportPolicy.summaryOf(dishes, days))
    }
}
