package com.gabrieltagama.menuplanner.core.data.share

import com.gabrieltagama.menuplanner.core.data.database.entity.MealDayEntity
import com.gabrieltagama.menuplanner.core.data.fake.TestData
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the last-writer-wins rule, the importability of meal days and MergeCount aggregation.
 */
class MergePolicyTest {
    private val date = LocalDate.of(2026, 3, 10)

    @Test
    fun `unknown item is added`() = assertEquals(MergeDecision.ADD, MergePolicy.decide(localUpdatedAt = null, incomingUpdatedAt = 10L))

    @Test
    fun `newer incoming item is updated`() = assertEquals(MergeDecision.UPDATE, MergePolicy.decide(localUpdatedAt = 10L, incomingUpdatedAt = 11L))

    @Test
    fun `older incoming item is skipped`() = assertEquals(MergeDecision.SKIP, MergePolicy.decide(localUpdatedAt = 10L, incomingUpdatedAt = 9L))

    @Test
    fun `incoming item with equal timestamp is skipped`() = assertEquals(MergeDecision.SKIP, MergePolicy.decide(localUpdatedAt = 10L, incomingUpdatedAt = 10L))

    @Test
    fun `single day with known dishes is importable`() =
        assertTrue(MergePolicy.isImportable(TestData.singleDay(date, "s", dessertId = "d"), setOf("s", "d")))

    @Test
    fun `courses day with known dishes is importable`() =
        assertTrue(MergePolicy.isImportable(TestData.coursesDay(date, "a", "b"), setOf("a", "b")))

    @Test
    fun `day referencing unknown dish is not importable`() =
        assertFalse(MergePolicy.isImportable(TestData.coursesDay(date, "a", "missing"), setOf("a")))

    @Test
    fun `day referencing unknown dessert is not importable`() =
        assertFalse(MergePolicy.isImportable(TestData.singleDay(date, "s", dessertId = "missing"), setOf("s")))

    @Test
    fun `day with starter but no main is not importable`() {
        val day = MealDayEntity(date.toEpochDay(), starterId = "a", mainId = null, singleId = null, dessertId = null, updatedAt = 1L)

        assertFalse(MergePolicy.isImportable(day, setOf("a")))
    }

    @Test
    fun `day with only dessert is not importable`() {
        val day = MealDayEntity(date.toEpochDay(), starterId = null, mainId = null, singleId = null, dessertId = "d", updatedAt = 1L)

        assertFalse(MergePolicy.isImportable(day, setOf("d")))
    }

    @Test
    fun `merge count aggregates decisions`() {
        val decisions = listOf(MergeDecision.ADD, MergeDecision.SKIP, MergeDecision.ADD, MergeDecision.UPDATE, MergeDecision.SKIP, MergeDecision.SKIP)

        assertEquals(MergeCount(added = 2, updated = 1, skipped = 3), MergeCount.of(decisions))
    }
}
