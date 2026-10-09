package com.gabrieltagama.menuplanner.core.data.share

import com.gabrieltagama.menuplanner.core.data.database.entity.MealDayEntity
import com.gabrieltagama.menuplanner.core.data.fake.TestData
import com.gabrieltagama.menuplanner.core.domain.model.DishType
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
        assertTrue(MergePolicy.isImportable(TestData.singleDay(date, "s", dessertId = "d"), mapOf("s" to DishType.SINGLE, "d" to DishType.DESSERT)))

    @Test
    fun `courses day with known dishes is importable`() =
        assertTrue(MergePolicy.isImportable(TestData.coursesDay(date, "a", "b"), mapOf("a" to DishType.STARTER, "b" to DishType.MAIN)))

    @Test
    fun `day referencing unknown dish is not importable`() =
        assertFalse(MergePolicy.isImportable(TestData.coursesDay(date, "a", "missing"), mapOf("a" to DishType.STARTER)))

    @Test
    fun `day referencing unknown dessert is not importable`() =
        assertFalse(MergePolicy.isImportable(TestData.singleDay(date, "s", dessertId = "missing"), mapOf("s" to DishType.SINGLE)))

    @Test
    fun `day with starter but no main is not importable`() {
        val day = MealDayEntity(date.toEpochDay(), starterId = "a", mainId = null, singleId = null, dessertId = null, updatedAt = 1L)

        assertFalse(MergePolicy.isImportable(day, mapOf("a" to DishType.STARTER)))
    }

    @Test
    fun `day with only dessert is not importable`() {
        val day = MealDayEntity(date.toEpochDay(), starterId = null, mainId = null, singleId = null, dessertId = "d", updatedAt = 1L)

        assertFalse(MergePolicy.isImportable(day, mapOf("d" to DishType.DESSERT)))
    }

    @Test
    fun `day with single and courses at once is not importable`() {
        val day = MealDayEntity(date.toEpochDay(), starterId = "a", mainId = "b", singleId = "s", dessertId = null, updatedAt = 1L)
        val types = mapOf("a" to DishType.STARTER, "b" to DishType.MAIN, "s" to DishType.SINGLE)

        assertFalse(MergePolicy.isImportable(day, types))
    }

    @Test
    fun `dessert placed as starter is not importable`() =
        assertFalse(MergePolicy.isImportable(TestData.coursesDay(date, "d", "b"), mapOf("d" to DishType.DESSERT, "b" to DishType.MAIN)))

    @Test
    fun `main placed as single is not importable`() =
        assertFalse(MergePolicy.isImportable(TestData.singleDay(date, "b"), mapOf("b" to DishType.MAIN)))

    @Test
    fun `starter placed as dessert is not importable`() =
        assertFalse(MergePolicy.isImportable(TestData.singleDay(date, "s", dessertId = "a"), mapOf("s" to DishType.SINGLE, "a" to DishType.STARTER)))

    @Test
    fun `merge count aggregates decisions`() {
        val decisions = listOf(MergeDecision.ADD, MergeDecision.SKIP, MergeDecision.ADD, MergeDecision.UPDATE, MergeDecision.SKIP, MergeDecision.SKIP)

        assertEquals(MergeCount(added = 2, updated = 1, skipped = 3), MergeCount.of(decisions))
    }
}
