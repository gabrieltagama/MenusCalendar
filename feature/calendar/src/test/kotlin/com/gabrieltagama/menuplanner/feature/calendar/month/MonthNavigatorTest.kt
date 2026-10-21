package com.gabrieltagama.menuplanner.feature.calendar.month

import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * MonthNavigator: starts on today, moving to another month selects its first day, coming back to
 * the current month selects today, and the days of the month cover the whole visible month.
 */
class MonthNavigatorTest {
    private val today = LocalDate.of(2026, 10, 21)
    private val navigator = MonthNavigator(Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC))

    @Test
    fun `starts on the current month with today selected`() {
        assertEquals(YearMonth.of(2026, 10), navigator.month.value)
        assertEquals(today, navigator.selectedDate.value)
        assertEquals(today, navigator.today.value)
    }

    @Test
    fun `another month selects its first day and the current one selects today`() {
        navigator.next()
        assertEquals(LocalDate.of(2026, 11, 1), navigator.selectedDate.value)

        navigator.previous()
        assertEquals(today, navigator.selectedDate.value)
    }

    @Test
    fun `going to today comes back from any month`() {
        navigator.previous()
        navigator.previous()
        navigator.select(LocalDate.of(2026, 8, 15))

        navigator.goToToday()

        assertEquals(YearMonth.of(2026, 10), navigator.month.value)
        assertEquals(today, navigator.selectedDate.value)
    }

    @Test
    fun `days of the month cover the visible month`() {
        assertEquals(31, navigator.daysOfMonth.size)
        assertEquals(LocalDate.of(2026, 10, 1), navigator.daysOfMonth.min())
    }
}
