package com.gabrieltagama.menuplanner.core.domain.port

import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.model.PlannedMeal
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

/**
 * Persistence port shared by the lunch and dinner calendars. observeMonth refreshes when a planned
 * dish changes; getRange is a one-shot read of a date range (both ends included, it may span
 * several months); saveAll stores many days at once and atomically in real implementations, the
 * default saving them one by one is only meant for simple fakes. Implemented in :core:data.
 */
interface PlanRepository<T : PlannedMeal> {
    fun observeMonth(month: YearMonth): Flow<List<T>>
    suspend fun getRange(dates: ClosedRange<LocalDate>): List<T>
    suspend fun getDay(date: LocalDate): T?
    suspend fun save(day: T)
    suspend fun saveAll(days: List<T>) = days.forEach { save(it) }
    suspend fun clear(date: LocalDate)
}

interface MealPlanRepository : PlanRepository<MealDay>

interface DinnerPlanRepository : PlanRepository<DinnerDay>
