package com.gabrieltagama.menuplanner.feature.calendar.common

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Spanish date texts of the calendar: month title ("Octubre 2026") and long day title
 * ("viernes, 9 de octubre").
 */
internal val spanishLocale: Locale = Locale.forLanguageTag("es-ES")

private val monthTitleFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", spanishLocale)

private val dayTitleFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", spanishLocale)

internal fun YearMonth.titleText(): String =
    format(monthTitleFormatter).replaceFirstChar { it.titlecase(spanishLocale) }

internal fun LocalDate.longTitleText(): String = format(dayTitleFormatter)
