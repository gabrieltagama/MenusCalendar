package com.gabrieltagama.menuplanner.feature.calendar.month

import com.gabrieltagama.menuplanner.core.domain.model.AutoFillResult
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.model.plan.PlannedDinner
import com.gabrieltagama.menuplanner.core.domain.model.plan.PlannedLunch
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.UndoableChange
import java.time.LocalDate
import java.time.YearMonth

/**
 * State of the monthly calendar: visible month, selected day, active view (lunches or dinners),
 * the planned lunches and dinners of the month, the autofill status and the interaction mode
 * (CalendarMode). selection and clipboard are read from the mode, so at most one of them exists.
 * CalendarEvent carries the one-shot results shown in a snackbar.
 */
data class CalendarUiState(
    val month: YearMonth,
    val selectedDate: LocalDate,
    val today: LocalDate,
    val view: Meal = Meal.LUNCH,
    val isLoading: Boolean = true,
    val lunches: Map<LocalDate, PlannedLunch> = emptyMap(),
    val dinners: Map<LocalDate, PlannedDinner> = emptyMap(),
    val isAutoFilling: Boolean = false,
    val showAutoFillConfirm: Boolean = false,
    val mode: CalendarMode = CalendarMode.Browsing
) {
    val selection: DaySelection? get() = (mode as? CalendarMode.Selecting)?.selection

    val clipboard: PlanClipboard? get() = (mode as? CalendarMode.Pasting)?.clipboard

    val isSelecting: Boolean get() = mode is CalendarMode.Selecting

    val isPasting: Boolean get() = mode is CalendarMode.Pasting

    val plannedDates: Set<LocalDate> get() = when (view) {
        Meal.LUNCH -> lunches.keys
        Meal.DINNER -> dinners.keys
    }

    val selectedLunch: PlannedLunch? get() = lunches[selectedDate]

    val selectedDinner: PlannedDinner? get() = dinners[selectedDate]

    val isShowingCurrentMonth: Boolean get() = month == YearMonth.from(today)

    val canAutoFill: Boolean get() = mode == CalendarMode.Browsing && !isLoading && !isAutoFilling

    fun isMarked(date: LocalDate): Boolean = selection?.dates?.contains(date) == true

    fun isPlanned(date: LocalDate): Boolean = date in plannedDates
}

sealed interface CalendarEvent {
    data class AutoFillCompleted(val result: AutoFillResult, val view: Meal) : CalendarEvent

    data class DaysDeleted(val change: UndoableChange) : CalendarEvent

    data class SelectionFilled(val result: AutoFillResult, val view: Meal) : CalendarEvent

    data class DaysPasted(val change: UndoableChange) : CalendarEvent

    data object NothingPlanned : CalendarEvent

    data object StorageFailed : CalendarEvent
}
