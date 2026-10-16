package com.gabrieltagama.menuplanner.feature.calendar.month

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * DaySelection: tapping toggles any day, dragging marks the continuous range in either direction
 * on top of the previous selection, and a drag started on a marked day unmarks the range.
 */
class DaySelectionTest {
    private fun day(dayOfMonth: Int) = LocalDate.of(2026, 10, dayOfMonth)

    @Test
    fun `tap toggles a day`() {
        val selection = DaySelection().toggle(day(2)).toggle(day(4))

        assertEquals(setOf(day(2), day(4)), selection.dates)
        assertEquals(setOf(day(4)), selection.toggle(day(2)).dates)
    }

    @Test
    fun `drag marks the continuous range`() {
        val selection = DaySelection().startDrag(day(2)).dragTo(day(6)).endDrag()

        assertEquals((2..6).map(::day).toSet(), selection.dates)
    }

    @Test
    fun `dragging backwards shrinks and reverses the range`() {
        val selection = DaySelection().startDrag(day(10)).dragTo(day(14)).dragTo(day(8))

        assertEquals(setOf(day(8), day(9), day(10)), selection.dates)
    }

    @Test
    fun `drag keeps days marked before it`() {
        val selection = DaySelection(setOf(day(20))).startDrag(day(1)).dragTo(day(2))

        assertEquals(setOf(day(1), day(2), day(20)), selection.dates)
    }

    @Test
    fun `drag starting on a marked day unmarks the range`() {
        val month = (1..31).map(::day).toSet()
        val marked = DaySelection().selectAll(month)

        val selection = marked.startDrag(day(10)).dragTo(day(12)).endDrag()

        assertEquals(month - setOf(day(10), day(11), day(12)), selection.dates)
    }

    @Test
    fun `moving without a drag changes nothing`() {
        assertEquals(setOf(day(3)), DaySelection(setOf(day(3))).dragTo(day(9)).dates)
    }
}
