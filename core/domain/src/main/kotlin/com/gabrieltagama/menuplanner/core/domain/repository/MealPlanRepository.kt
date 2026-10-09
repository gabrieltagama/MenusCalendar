package com.gabrieltagama.menuplanner.core.domain.repository

import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

/**
 * Persistence port for the monthly calendar. Implemented in :core:data.
 */
interface MealPlanRepository {
    fun observeMonth(month: YearMonth): Flow<List<MealDay>>
    suspend fun getDay(date: LocalDate): MealDay?
    suspend fun save(day: MealDay)
    suspend fun clear(date: LocalDate)
}
