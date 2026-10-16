package com.gabrieltagama.menuplanner.core.domain.model

import java.time.LocalDate

/**
 * Rules shared by the lunch and dinner autofill: a dish served within RECENT_DAYS days before or
 * after a date counts as recent, and a lunch with a VERY_HIGH dish is a heavy day that should not
 * be next to another one.
 */
object PlanningPolicy {
    const val RECENT_DAYS = 6L

    fun nearbyDates(date: LocalDate): List<LocalDate> = (1..RECENT_DAYS).flatMap { listOf(date.minusDays(it), date.plusDays(it)) }

    fun window(dates: ClosedRange<LocalDate>): ClosedRange<LocalDate> =
        dates.start.minusDays(RECENT_DAYS)..dates.endInclusive.plusDays(RECENT_DAYS)

    fun isHeavy(dish: Dish): Boolean = dish.heaviness == Heaviness.VERY_HIGH

    fun isHeavyDay(menu: DailyMenu?): Boolean = menu?.dishes.orEmpty().any(::isHeavy)
}
