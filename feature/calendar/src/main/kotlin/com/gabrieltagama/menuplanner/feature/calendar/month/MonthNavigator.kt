package com.gabrieltagama.menuplanner.feature.calendar.month

import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Navigation state of the monthly calendar: the visible month, the selected day and "today". Moving
 * to another month selects today when it is the current month and the first day otherwise; "today"
 * is read again from the clock on every move and whenever refreshToday is called (screen resume),
 * so it stays right after midnight.
 */
internal class MonthNavigator(private val clock: Clock) {
    private val visibleMonth = MutableStateFlow(YearMonth.now(clock))
    private val selected = MutableStateFlow(LocalDate.now(clock))
    private val currentDay = MutableStateFlow(LocalDate.now(clock))

    val month: StateFlow<YearMonth> = visibleMonth.asStateFlow()
    val selectedDate: StateFlow<LocalDate> = selected.asStateFlow()
    val today: StateFlow<LocalDate> = currentDay.asStateFlow()

    val daysOfMonth: Set<LocalDate> get() = month.value.let { current -> (1..current.lengthOfMonth()).map(current::atDay).toSet() }

    fun previous() = show(month.value.minusMonths(1))

    fun next() = show(month.value.plusMonths(1))

    fun goToToday() {
        val current = refreshToday()
        visibleMonth.value = YearMonth.from(current)
        selected.value = current
    }

    fun select(date: LocalDate) {
        selected.value = date
    }

    fun refreshToday(): LocalDate = LocalDate.now(clock).also { currentDay.value = it }

    private fun show(target: YearMonth) {
        val current = refreshToday()
        visibleMonth.value = target
        selected.value = if (YearMonth.from(current) == target) current else target.atDay(1)
    }
}
