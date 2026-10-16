package com.gabrieltagama.menuplanner.core.domain.repository

import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

/**
 * Persistence port for the lunch calendar. getRange reads the planned days between two dates (both
 * included) once; saveAll writes several days in a single transaction;
 * clearAll removes several days at once. Implemented in :core:data.
 */
interface MealPlanRepository {
    fun observeMonth(month: YearMonth): Flow<List<MealDay>>
    suspend fun getDay(date: LocalDate): MealDay?
    suspend fun getRange(dates: ClosedRange<LocalDate>): List<MealDay>
    suspend fun save(day: MealDay)
    suspend fun saveAll(days: List<MealDay>)
    suspend fun clear(date: LocalDate)
    suspend fun clearAll(dates: Collection<LocalDate>)
}
