package com.gabrieltagama.menuplanner.core.domain.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RepeatWindow: values are kept between 0 and 14, nearby dates cover the days on both sides, and the
 * history range always reaches at least one day around for the heavy-day rule.
 */
class RepeatWindowTest {
    private val date = LocalDate.of(2026, 3, 15)

    @Test
    fun `values outside the limits are coerced`() {
        assertEquals(0, RepeatWindow.of(-3).days)
        assertEquals(14, RepeatWindow.of(30).days)
        assertEquals(6, RepeatWindow.DEFAULT.days)
    }

    @Test
    fun `nearby dates cover both sides of the date`() {
        val nearby = RepeatWindow.of(2).nearbyDates(date).toSet()

        assertEquals(setOf(date.minusDays(2), date.minusDays(1), date.plusDays(1), date.plusDays(2)), nearby)
        assertTrue(RepeatWindow.of(0).nearbyDates(date).isEmpty())
    }

    @Test
    fun `history range widens by the window and at least one day`() {
        assertEquals(date.minusDays(3)..date.plusDays(3), RepeatWindow.of(3).historyRange(date..date))
        assertEquals(date.minusDays(1)..date.plusDays(1), RepeatWindow.of(0).historyRange(date..date))
    }

    @Test
    fun `limits disable decreasing and increasing`() {
        assertFalse(RepeatWindow.of(0).canDecrease())
        assertTrue(RepeatWindow.of(0).canIncrease())
        assertFalse(RepeatWindow.of(14).canIncrease())
    }
}
