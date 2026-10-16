package com.gabrieltagama.menuplanner.core.domain.query

import com.gabrieltagama.menuplanner.core.domain.model.plan.PlannedDinner
import com.gabrieltagama.menuplanner.core.domain.model.plan.PlannedLunch
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

/**
 * Read port (CQRS query side) of the monthly calendar: the lunches and dinners planned in a month,
 * sorted by date, as lightweight read models. Each flow emits again whenever the plan of that
 * month or one of its dishes changes. Implemented in :core:data.
 */
interface CalendarQueries {
    fun observeLunches(month: YearMonth): Flow<List<PlannedLunch>>
    fun observeDinners(month: YearMonth): Flow<List<PlannedDinner>>
}
