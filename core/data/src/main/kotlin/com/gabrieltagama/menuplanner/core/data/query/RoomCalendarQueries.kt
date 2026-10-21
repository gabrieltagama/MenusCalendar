package com.gabrieltagama.menuplanner.core.data.query

import com.gabrieltagama.menuplanner.core.data.database.dao.PlanQueryDao
import com.gabrieltagama.menuplanner.core.data.mapper.toPlannedDinner
import com.gabrieltagama.menuplanner.core.data.mapper.toPlannedLunch
import com.gabrieltagama.menuplanner.core.domain.model.plan.PlannedDinner
import com.gabrieltagama.menuplanner.core.domain.model.plan.PlannedLunch
import com.gabrieltagama.menuplanner.core.domain.query.CalendarQueries
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Room-backed read side of the calendar: one joined query per month and meal, mapped to read
 * models. Emissions that leave the month unchanged (an edit to an unplanned dish) are dropped.
 */
internal class RoomCalendarQueries @Inject constructor(private val dao: PlanQueryDao) : CalendarQueries {

    override fun observeLunches(month: YearMonth): Flow<List<PlannedLunch>> =
        dao.observeLunchRows(month.atDay(1), month.atEndOfMonth())
            .map { rows -> rows.mapNotNull { it.toPlannedLunch() } }
            .distinctUntilChanged()

    override fun observeDinners(month: YearMonth): Flow<List<PlannedDinner>> =
        dao.observeDinnerRows(month.atDay(1), month.atEndOfMonth())
            .map { rows -> rows.map { it.toPlannedDinner() } }
            .distinctUntilChanged()
}
