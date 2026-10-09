package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.repository.MealPlanRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory MealPlanRepository keyed by date, recording saved days and cleared dates.
 */
class FakeMealPlanRepository(initial: List<MealDay> = emptyList()) : MealPlanRepository {
    private val days = MutableStateFlow(initial.associateBy { it.date })
    val saved = mutableListOf<MealDay>()
    val cleared = mutableListOf<LocalDate>()

    val current: Map<LocalDate, MealDay> get() = days.value

    override fun observeMonth(month: YearMonth): Flow<List<MealDay>> =
        days.map { all -> all.values.filter { YearMonth.from(it.date) == month }.sortedBy { it.date } }

    override suspend fun getDay(date: LocalDate): MealDay? = days.value[date]

    override suspend fun save(day: MealDay) {
        saved += day
        days.value = days.value + (day.date to day)
    }

    override suspend fun clear(date: LocalDate) {
        cleared += date
        days.value = days.value - date
    }
}
