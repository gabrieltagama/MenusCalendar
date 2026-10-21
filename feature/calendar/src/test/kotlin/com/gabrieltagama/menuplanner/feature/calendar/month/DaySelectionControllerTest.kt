package com.gabrieltagama.menuplanner.feature.calendar.month

import com.gabrieltagama.menuplanner.core.domain.model.Meal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DaySelectionController: transitions between browsing, selecting and pasting; a long press drag
 * starts a selection except while pasting; taking or copying an empty selection keeps it open;
 * taking the clipboard and cancelling return to browsing, and calls the mode does not allow are
 * ignored.
 */
class DaySelectionControllerTest {

    private val controller = DaySelectionController()

    @Test
    fun `starts browsing and start opens an empty selection`() {
        assertEquals(CalendarMode.Browsing, controller.mode.value)

        controller.start()

        assertEquals(CalendarMode.Selecting(DaySelection()), controller.mode.value)
    }

    @Test
    fun `toggle and select all only act while selecting`() {
        controller.toggle(day(1))
        controller.selectAll(setOf(day(2)))
        assertEquals(CalendarMode.Browsing, controller.mode.value)

        controller.start()
        controller.toggle(day(1))
        controller.selectAll(setOf(day(2), day(3)))

        assertEquals(setOf(day(1), day(2), day(3)), marked())
    }

    @Test
    fun `drag from browsing opens the selection with the range`() {
        controller.startDrag(day(2))
        controller.dragTo(day(4))
        controller.endDrag()

        assertEquals((2..4).map(::day).toSet(), marked())
    }

    @Test
    fun `take returns the marked days and goes back to browsing`() {
        controller.start()
        controller.toggle(day(5))

        assertEquals(setOf(day(5)), controller.take())
        assertEquals(CalendarMode.Browsing, controller.mode.value)
    }

    @Test
    fun `take and copy of an empty selection keep it open`() {
        controller.start()

        assertTrue(controller.take().isEmpty())
        assertNull(controller.copy(Meal.LUNCH))
        assertTrue(controller.mode.value is CalendarMode.Selecting)
    }

    @Test
    fun `copy switches to pasting and blocks drags`() {
        controller.start()
        controller.toggle(day(1))

        val clipboard = controller.copy(Meal.DINNER)
        controller.startDrag(day(9))

        assertEquals(PlanClipboard(setOf(day(1)), Meal.DINNER), clipboard)
        assertEquals(CalendarMode.Pasting(PlanClipboard(setOf(day(1)), Meal.DINNER)), controller.mode.value)
    }

    @Test
    fun `take clipboard returns the copy once`() {
        controller.start()
        controller.toggle(day(1))
        controller.copy(Meal.LUNCH)

        assertEquals(PlanClipboard(setOf(day(1)), Meal.LUNCH), controller.takeClipboard())
        assertNull(controller.takeClipboard())
        assertEquals(CalendarMode.Browsing, controller.mode.value)
    }

    @Test
    fun `start while pasting drops the copy`() {
        controller.start()
        controller.toggle(day(1))
        controller.copy(Meal.LUNCH)

        controller.start()

        assertEquals(CalendarMode.Selecting(DaySelection()), controller.mode.value)
    }

    @Test
    fun `each cancel only leaves its own mode`() {
        controller.start()
        controller.cancelPaste()
        assertTrue(controller.mode.value is CalendarMode.Selecting)
        controller.cancel()
        assertEquals(CalendarMode.Browsing, controller.mode.value)

        controller.start()
        controller.toggle(day(1))
        controller.copy(Meal.LUNCH)
        controller.cancel()
        assertTrue(controller.mode.value is CalendarMode.Pasting)
        controller.cancelPaste()
        assertEquals(CalendarMode.Browsing, controller.mode.value)
    }

    private fun marked(): Set<LocalDate> = (controller.mode.value as CalendarMode.Selecting).selection.dates

    private fun day(dayOfMonth: Int): LocalDate = LocalDate.of(2026, 10, dayOfMonth)
}
