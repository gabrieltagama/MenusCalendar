package com.gabrieltagama.menuplanner.feature.calendar.month

import java.time.LocalDate

/**
 * Days marked in the calendar selection mode. A tap toggles one day; a drag marks the continuous
 * range (in reading order) between the day where it started and the day under the finger, on top
 * of what was marked before the drag. A drag that starts on a marked day unmarks that range instead.
 */
data class DaySelection(
    val dates: Set<LocalDate> = emptySet(),
    private val drag: DragRange? = null
) {
    val count: Int get() = dates.size

    fun toggle(date: LocalDate): DaySelection = copy(dates = if (date in dates) dates - date else dates + date)

    fun selectAll(days: Set<LocalDate>): DaySelection = copy(dates = dates + days)

    fun startDrag(anchor: LocalDate): DaySelection =
        copy(drag = DragRange(anchor = anchor, baseline = dates, isAdding = anchor !in dates)).dragTo(anchor)

    fun dragTo(target: LocalDate): DaySelection {
        val range = drag ?: return this
        val covered = range.datesTo(target).toSet()
        return copy(dates = if (range.isAdding) range.baseline + covered else range.baseline - covered)
    }

    fun endDrag(): DaySelection = copy(drag = null)
}

data class DragRange(val anchor: LocalDate, val baseline: Set<LocalDate>, val isAdding: Boolean) {
    fun datesTo(target: LocalDate): List<LocalDate> {
        val (from, to) = if (target < anchor) target to anchor else anchor to target
        return generateSequence(from) { it.plusDays(1) }.takeWhile { it <= to }.toList()
    }
}
