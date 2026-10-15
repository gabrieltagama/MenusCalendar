package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.model.PlannedMeal
import com.gabrieltagama.menuplanner.core.domain.repository.DinnerPlanRepository
import com.gabrieltagama.menuplanner.core.domain.repository.MealPlanRepository
import com.gabrieltagama.menuplanner.core.domain.repository.PlanRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory PlanRepository keyed by date, recording saved days, saveAll batches and cleared dates.
 * FakeMealPlanRepository and FakeDinnerPlanRepository bind it to each calendar.
 */
open class FakePlanRepository<T : PlannedMeal>(initial: List<T>) : PlanRepository<T> {
    private val days = MutableStateFlow(initial.associateBy { it.date })
    val saved = mutableListOf<T>()
    val batches = mutableListOf<List<T>>()
    val cleared = mutableListOf<LocalDate>()

    val current: Map<LocalDate, T> get() = days.value

    override fun observeMonth(month: YearMonth): Flow<List<T>> =
        days.map { all -> all.values.filter { YearMonth.from(it.date) == month }.sortedBy { it.date } }

    override suspend fun getRange(dates: ClosedRange<LocalDate>): List<T> =
        days.value.values.filter { it.date in dates }.sortedBy { it.date }

    override suspend fun getDay(date: LocalDate): T? = days.value[date]

    override suspend fun save(day: T) {
        saved += day
        days.value = days.value + (day.date to day)
    }

    override suspend fun saveAll(days: List<T>) {
        batches += days
        days.forEach { save(it) }
    }

    override suspend fun clear(date: LocalDate) {
        cleared += date
        days.value = days.value - date
    }
}

class FakeMealPlanRepository(initial: List<MealDay> = emptyList()) : FakePlanRepository<MealDay>(initial), MealPlanRepository

class FakeDinnerPlanRepository(initial: List<DinnerDay> = emptyList()) : FakePlanRepository<DinnerDay>(initial), DinnerPlanRepository
