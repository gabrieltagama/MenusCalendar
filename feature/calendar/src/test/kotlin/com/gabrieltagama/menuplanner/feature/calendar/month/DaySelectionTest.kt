package com.gabrieltagama.menuplanner.feature.calendar.month

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * DaySelection: tapping toggles only selectable days, dragging marks the continuous range in either
 * direction on top of the previous selection, and a drag started on a marked day unmarks the range.
 */
class DaySelectionTest {
    private fun day(dayOfMonth: Int) = LocalDate.of(2026, 10, dayOfMonth)

    private val selectable = (1..31).filter { it != 4 }.map(::day).toSet()

    @Test
    fun `tap toggles selectable days and ignores the rest`() {
        val selection = DaySelection().toggle(day(2), selectable).toggle(day(4), selectable)

        assertEquals(setOf(day(2)), selection.dates)
        assertEquals(emptySet<LocalDate>(), selection.toggle(day(2), selectable).dates)
    }

    @Test
    fun `drag marks the continuous range skipping non selectable days`() {
        val selection = DaySelection().startDrag(day(2), selectable).dragTo(day(6), selectable).endDrag()

        assertEquals(setOf(day(2), day(3), day(5), day(6)), selection.dates)
    }

    @Test
    fun `dragging backwards shrinks and reverses the range`() {
        val selection = DaySelection().startDrag(day(10), selectable).dragTo(day(14), selectable).dragTo(day(8), selectable)

        assertEquals(setOf(day(8), day(9), day(10)), selection.dates)
    }

    @Test
    fun `drag keeps days marked before it`() {
        val selection = DaySelection(setOf(day(20))).startDrag(day(1), selectable).dragTo(day(2), selectable)

        assertEquals(setOf(day(1), day(2), day(20)), selection.dates)
    }

    @Test
    fun `drag starting on a marked day unmarks the range`() {
        val marked = DaySelection().selectAll(selectable)

        val selection = marked.startDrag(day(10), selectable).dragTo(day(12), selectable).endDrag()

        assertEquals(selectable - setOf(day(10), day(11), day(12)), selection.dates)
    }
}
