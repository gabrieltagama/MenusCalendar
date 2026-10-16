package com.gabrieltagama.menuplanner.feature.calendar.month

import java.time.LocalDate

/**
 * Days marked for deletion in the calendar selection mode. A tap toggles one day; a drag marks the
 * continuous range (in reading order) between the day where it started and the day under the
 * finger, on top of what was marked before the drag. A drag that starts on a marked day unmarks
 * that range instead. Only selectable days (planned in the active view) can be marked.
 */
data class DaySelection(
    val dates: Set<LocalDate> = emptySet(),
    private val drag: DragRange? = null
) {
    val count: Int get() = dates.size

    fun toggle(date: LocalDate, selectable: Set<LocalDate>): DaySelection = when (date) {
        in dates -> copy(dates = dates - date)
        in selectable -> copy(dates = dates + date)
        else -> this
    }

    fun selectAll(selectable: Set<LocalDate>): DaySelection = copy(dates = dates + selectable)

    fun startDrag(anchor: LocalDate, selectable: Set<LocalDate>): DaySelection =
        copy(drag = DragRange(anchor = anchor, baseline = dates, isAdding = anchor !in dates)).dragTo(anchor, selectable)

    fun dragTo(target: LocalDate, selectable: Set<LocalDate>): DaySelection {
        val range = drag ?: return this
        val covered = range.datesTo(target).filter { it in selectable }.toSet()
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
