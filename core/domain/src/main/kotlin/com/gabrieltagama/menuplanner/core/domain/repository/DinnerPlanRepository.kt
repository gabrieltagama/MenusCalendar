package com.gabrieltagama.menuplanner.core.domain.repository

import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import java.time.LocalDate

/**
 * Persistence port for the dinner calendar. getRange reads the planned dinners between two dates
 * (both included) once; saveAll writes several dinners in a single transaction;
 * clearAll removes several dinners at once. Implemented in
 * :core:data.
 */
interface DinnerPlanRepository {
    suspend fun getDay(date: LocalDate): DinnerDay?
    suspend fun getRange(dates: ClosedRange<LocalDate>): List<DinnerDay>
    suspend fun save(day: DinnerDay)
    suspend fun saveAll(days: List<DinnerDay>)
    suspend fun clear(date: LocalDate)
    suspend fun clearAll(dates: Collection<LocalDate>)
}
