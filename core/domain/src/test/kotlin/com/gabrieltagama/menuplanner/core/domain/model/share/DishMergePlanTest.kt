package com.gabrieltagama.menuplanner.core.domain.model.share

import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests how incoming dishes are matched to the local recipe book: by id first, by name and type
 * when the id is unknown (keeping the local id), newest version wins, duplicates in the same
 * import collapse into one dish, and planned days are remapped to the local ids.
 */
class DishMergePlanTest {
    private val older = Instant.parse("2026-01-01T00:00:00Z")
    private val newer = Instant.parse("2026-02-01T00:00:00Z")

    @Test
    fun `unknown dish is added with its own id`() {
        val incoming = TestDishes.dish(id = "remote", name = "Paella", updatedAt = older)

        val plan = DishMergePlan.of(local = emptyList(), incoming = listOf(incoming))

        assertEquals(listOf(MergeDecision.ADD), plan.decisions)
        assertEquals(listOf(incoming), plan.writes)
        assertEquals(DishId("remote"), plan.idMap[DishId("remote")])
    }

    @Test
    fun `newer dish with the same name and type updates the local one keeping its id`() {
        val local = TestDishes.dish(id = "local", name = "Lentejas", type = DishType.SINGLE, updatedAt = older)
        val incoming = TestDishes.dish(id = "remote", name = "lentejas ", type = DishType.SINGLE, preparation = "New", updatedAt = newer)

        val plan = DishMergePlan.of(local = listOf(local), incoming = listOf(incoming))

        assertEquals(listOf(MergeDecision.UPDATE), plan.decisions)
        assertEquals(listOf(incoming.copy(id = local.id)), plan.writes)
        assertEquals(local.id, plan.idMap[incoming.id])
    }

    @Test
    fun `older dish with the same name and type is skipped but still mapped`() {
        val local = TestDishes.dish(id = "local", name = "Lentejas", type = DishType.SINGLE, updatedAt = newer)
        val incoming = TestDishes.dish(id = "remote", name = "Lentejas", type = DishType.SINGLE, updatedAt = older)

        val plan = DishMergePlan.of(local = listOf(local), incoming = listOf(incoming))

        assertEquals(listOf(MergeDecision.SKIP), plan.decisions)
        assertTrue(plan.writes.isEmpty())
        assertEquals(local.id, plan.idMap[incoming.id])
    }

    @Test
    fun `same name with another type is a different dish`() {
        val local = TestDishes.dish(id = "local", name = "Ensalada", type = DishType.STARTER, updatedAt = older)
        val incoming = TestDishes.dish(id = "remote", name = "Ensalada", type = DishType.SINGLE, updatedAt = newer)

        val plan = DishMergePlan.of(local = listOf(local), incoming = listOf(incoming))

        assertEquals(listOf(MergeDecision.ADD), plan.decisions)
        assertEquals(incoming.id, plan.idMap[incoming.id])
    }

    @Test
    fun `match by id wins over match by name`() {
        val sameId = TestDishes.dish(id = "shared", name = "Old name", updatedAt = older)
        val sameName = TestDishes.dish(id = "other", name = "New name", updatedAt = older)
        val incoming = TestDishes.dish(id = "shared", name = "New name", updatedAt = newer)

        val plan = DishMergePlan.of(local = listOf(sameId, sameName), incoming = listOf(incoming))

        assertEquals(listOf(incoming), plan.writes)
        assertEquals(sameId.id, plan.idMap[incoming.id])
    }

    @Test
    fun `incoming duplicates by name collapse into the newest one`() {
        val newest = TestDishes.dish(id = "a", name = "Tortilla", updatedAt = newer)
        val oldest = TestDishes.dish(id = "b", name = "TORTILLA", updatedAt = older)

        val plan = DishMergePlan.of(local = emptyList(), incoming = listOf(oldest, newest))

        assertEquals(listOf(newest), plan.writes)
        assertEquals(listOf(MergeDecision.ADD, MergeDecision.SKIP), plan.decisions)
        assertEquals(newest.id, plan.idMap[oldest.id])
    }

    @Test
    fun `planned days are remapped to the local ids`() {
        val idMap = mapOf(DishId("remote") to DishId("local"))
        val date = LocalDate.of(2026, 3, 10)

        assertEquals(DishId("local"), LunchEntry(date, singleId = DishId("remote"), updatedAt = older).remapped(idMap).singleId)
        assertEquals(DishId("local"), DinnerEntry(date, DishId("remote"), older).remapped(idMap).dishId)
        assertEquals(DishId("kept"), DinnerEntry(date, DishId("kept"), older).remapped(idMap).dishId)
    }
}
