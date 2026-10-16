package com.gabrieltagama.menuplanner.core.domain.model.share

import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Tests that the fingerprint ignores order but changes when a dish, lunch or dinner is added,
 * edited or removed.
 */
class SnapshotFingerprintTest {
    private val first = dish(id = "a")
    private val second = dish(id = "b")
    private val date = LocalDate.of(2026, 3, 10)
    private val lunch = LunchEntry(date, singleId = first.id, updatedAt = TestDishes.OLD_INSTANT)
    private val dinner = DinnerEntry(date, second.id, TestDishes.OLD_INSTANT)
    private val snapshot = MenuSnapshot(listOf(first, second), listOf(lunch), listOf(dinner))

    @Test
    fun `order does not matter`() =
        assertEquals(SnapshotFingerprint.of(snapshot), SnapshotFingerprint.of(snapshot.copy(dishes = listOf(second, first))))

    @Test
    fun `adding or removing a dish changes it`() =
        assertNotEquals(SnapshotFingerprint.of(snapshot), SnapshotFingerprint.of(snapshot.copy(dishes = listOf(first))))

    @Test
    fun `editing a dish changes it`() = assertNotEquals(
        SnapshotFingerprint.of(snapshot),
        SnapshotFingerprint.of(snapshot.copy(dishes = listOf(first.copy(updatedAt = TestDishes.FIXED_INSTANT), second)))
    )

    @Test
    fun `planning or changing the calendar changes it`() {
        assertNotEquals(SnapshotFingerprint.of(snapshot), SnapshotFingerprint.of(snapshot.copy(lunches = emptyList())))
        assertNotEquals(SnapshotFingerprint.of(snapshot), SnapshotFingerprint.of(snapshot.copy(dinners = listOf(dinner.copy(dishId = first.id)))))
    }
}
