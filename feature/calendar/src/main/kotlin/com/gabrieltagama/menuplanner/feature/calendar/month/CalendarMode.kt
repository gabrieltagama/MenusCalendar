package com.gabrieltagama.menuplanner.feature.calendar.month

import com.gabrieltagama.menuplanner.core.domain.model.Meal
import java.time.LocalDate

/**
 * Interaction mode of the monthly calendar. Only one mode exists at a time: browsing the month,
 * marking days (Selecting) or waiting for the first target day of a copy (Pasting), so a selection
 * and a pending copy can never coexist.
 */
sealed interface CalendarMode {
    data object Browsing : CalendarMode

    data class Selecting(val selection: DaySelection) : CalendarMode

    data class Pasting(val clipboard: PlanClipboard) : CalendarMode
}

data class PlanClipboard(val dates: Set<LocalDate>, val view: Meal) {
    val count: Int get() = dates.size
}
