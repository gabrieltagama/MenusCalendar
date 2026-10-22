package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.model.AutoFillResult
import com.gabrieltagama.menuplanner.core.domain.model.firstPlannableDay
import java.time.LocalDate
import java.time.YearMonth

/**
 * Skeleton shared by the lunch and dinner autofill: picks each empty day in date order (a pick may
 * see the days picked before it) and summarizes the run as an AutoFillResult. A null pick leaves
 * the day empty. The days of a whole month to fill start today when the month is the current one,
 * so past days are never filled.
 */
internal class DayPick<D>(val day: D, val repeated: Boolean, val breaksHeavyRule: Boolean = false)

internal class MonthFill<D>(val days: List<D>, val result: AutoFillResult)

internal inline fun <D> fillEmptyDays(emptyDays: List<LocalDate>, pick: (LocalDate) -> DayPick<D>?): MonthFill<D> {
    val picks = emptyDays.mapNotNull(pick)
    return MonthFill(
        days = picks.map { it.day },
        result = AutoFillResult(
            filledDays = picks.size,
            repeatedDishDays = picks.count { it.repeated },
            heavyStreakDays = picks.count { it.breaksHeavyRule },
            unfilledDays = emptyDays.size - picks.size
        )
    )
}

internal fun YearMonth.daysToFill(today: LocalDate): List<LocalDate> =
    (firstPlannableDay(today).dayOfMonth..lengthOfMonth()).map(::atDay)
