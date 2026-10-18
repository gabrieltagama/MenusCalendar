package com.gabrieltagama.menuplanner.feature.calendar.month

import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * State machine of the calendar selection mode, free of Android so it can be tested on its own.
 * Starting a selection drops a pending copy; a drag (long press) also starts it unless a copy is
 * waiting to be pasted. Taking or copying the marked days returns to browsing or pasting only when
 * at least one day is marked; otherwise the selection stays open. Every other call is ignored when
 * the current mode does not allow it.
 */
class DaySelectionController {

    private val _mode = MutableStateFlow<CalendarMode>(CalendarMode.Browsing)
    val mode: StateFlow<CalendarMode> = _mode.asStateFlow()

    private val markedDates: Set<LocalDate> get() = (_mode.value as? CalendarMode.Selecting)?.selection?.dates.orEmpty()

    fun start() = _mode.update { it as? CalendarMode.Selecting ?: CalendarMode.Selecting(DaySelection()) }

    fun cancel() = _mode.update { if (it is CalendarMode.Selecting) CalendarMode.Browsing else it }

    fun toggle(date: LocalDate) = updateSelection { it.toggle(date) }

    fun selectAll(dates: Set<LocalDate>) = updateSelection { it.selectAll(dates) }

    fun startDrag(date: LocalDate) = _mode.update { mode ->
        when (mode) {
            CalendarMode.Browsing -> CalendarMode.Selecting(DaySelection().startDrag(date))
            is CalendarMode.Selecting -> CalendarMode.Selecting(mode.selection.startDrag(date))
            is CalendarMode.Pasting -> mode
        }
    }

    fun dragTo(date: LocalDate) = updateSelection { it.dragTo(date) }

    fun endDrag() = updateSelection(DaySelection::endDrag)

    fun take(): Set<LocalDate> = markedDates.also { if (it.isNotEmpty()) _mode.value = CalendarMode.Browsing }

    fun copy(view: PlanView): PlanClipboard? =
        markedDates.takeIf(Set<LocalDate>::isNotEmpty)
            ?.let { PlanClipboard(it, view) }
            ?.also { _mode.value = CalendarMode.Pasting(it) }

    fun takeClipboard(): PlanClipboard? =
        (_mode.value as? CalendarMode.Pasting)?.clipboard?.also { _mode.value = CalendarMode.Browsing }

    fun cancelPaste() = _mode.update { if (it is CalendarMode.Pasting) CalendarMode.Browsing else it }

    private fun updateSelection(transform: (DaySelection) -> DaySelection) = _mode.update { mode ->
        if (mode is CalendarMode.Selecting) CalendarMode.Selecting(transform(mode.selection)) else mode
    }
}
