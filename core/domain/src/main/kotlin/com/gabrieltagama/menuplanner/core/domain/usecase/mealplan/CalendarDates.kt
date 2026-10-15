package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import java.time.LocalDate
import java.time.YearMonth

/**
 * Date helpers shared by the calendar use cases: every day of a month, and a month as a closed
 * date range.
 */
internal fun YearMonth.days(): List<LocalDate> = (1..lengthOfMonth()).map(::atDay)

internal fun YearMonth.dates(): ClosedRange<LocalDate> = atDay(1)..atEndOfMonth()
