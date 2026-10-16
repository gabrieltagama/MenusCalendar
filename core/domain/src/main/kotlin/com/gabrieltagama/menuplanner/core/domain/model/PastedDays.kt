package com.gabrieltagama.menuplanner.core.domain.model

import java.time.LocalDate

/**
 * Value object: result of pasting copied days of the calendar. pasted are the days written at
 * the target dates and replaced the days that were there before, so the paste can be undone.
 */
data class PastedDays<D>(val pasted: List<D>, val replaced: List<D>) {
    val count: Int get() = pasted.size
}

/**
 * Target date of each copied date: the copy keeps the gaps between days, and its first date
 * ([sourceStart]) lands on [targetStart].
 */
fun LocalDate.shifted(sourceStart: LocalDate, targetStart: LocalDate): LocalDate =
    plusDays(targetStart.toEpochDay() - sourceStart.toEpochDay())
