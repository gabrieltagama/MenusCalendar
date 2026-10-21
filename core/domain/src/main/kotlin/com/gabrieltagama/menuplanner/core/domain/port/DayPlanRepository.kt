package com.gabrieltagama.menuplanner.core.domain.port

import com.gabrieltagama.menuplanner.core.domain.model.PlannedDay
import java.time.LocalDate

/**
 * Persistence port shared by the lunch and dinner calendars. getRange reads the planned days
 * between two dates (both included) once; saveAll writes several days in a single transaction;
 * clearAll removes several days at once. MealPlanRepository and DinnerPlanRepository fix the type
 * of day, so each one can be injected on its own. Implemented in :core:data.
 */
interface DayPlanRepository<D : PlannedDay<D>> {
    suspend fun getDay(date: LocalDate): D?
    suspend fun getRange(dates: ClosedRange<LocalDate>): List<D>
    suspend fun save(day: D)
    suspend fun saveAll(days: List<D>)
    suspend fun clear(date: LocalDate)
    suspend fun clearAll(dates: Collection<LocalDate>)
}
