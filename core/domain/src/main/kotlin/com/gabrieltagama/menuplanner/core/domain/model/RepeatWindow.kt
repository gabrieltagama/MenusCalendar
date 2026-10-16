package com.gabrieltagama.menuplanner.core.domain.model

import java.time.LocalDate

/**
 * Value object of the user setting "Días sin repetir plato": a dish served within [days] days before
 * or after a date counts as recent for the lunch and dinner autofill. It is always between
 * [MIN_DAYS] and [MAX_DAYS] ([DEFAULT_DAYS] when never set). historyRange widens a range of dates
 * with the days whose plan must be read to apply the rules, at least one day on each side so the
 * heavy-day rule also holds across the edges.
 */
@JvmInline
value class RepeatWindow private constructor(val days: Int) {

    fun nearbyDates(date: LocalDate): List<LocalDate> = (1..days).flatMap { listOf(date.minusDays(it.toLong()), date.plusDays(it.toLong())) }

    fun historyRange(dates: ClosedRange<LocalDate>): ClosedRange<LocalDate> {
        val margin = maxOf(days, HEAVY_NEIGHBOUR_DAYS).toLong()
        return dates.start.minusDays(margin)..dates.endInclusive.plusDays(margin)
    }

    fun canDecrease(): Boolean = days > MIN_DAYS

    fun canIncrease(): Boolean = days < MAX_DAYS

    companion object {
        const val MIN_DAYS = 0
        const val MAX_DAYS = 14
        const val DEFAULT_DAYS = 6
        private const val HEAVY_NEIGHBOUR_DAYS = 1

        val DEFAULT: RepeatWindow = RepeatWindow(DEFAULT_DAYS)

        fun of(days: Int): RepeatWindow = RepeatWindow(days.coerceIn(MIN_DAYS, MAX_DAYS))
    }
}
