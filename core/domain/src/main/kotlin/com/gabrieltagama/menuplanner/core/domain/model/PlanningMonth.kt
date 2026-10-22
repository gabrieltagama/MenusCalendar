package com.gabrieltagama.menuplanner.core.domain.model

import java.time.LocalDate
import java.time.YearMonth

/**
 * First day of a month that can still be planned: today when the month is the current one, so past
 * days are never filled, otherwise the first day of the month. The autofill and the default range
 * of the shopping list share it.
 */
fun YearMonth.firstPlannableDay(today: LocalDate): LocalDate = if (YearMonth.from(today) == this) today else atDay(1)
