package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.repository.MealPlanRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory MealPlanRepository keyed by date, recording saved days, cleared dates and observed months.
 */
class FakeMealPlanRepository(initial: List<MealDay> = emptyList()) : MealPlanRepository {
    private val days = MutableStateFlow(initial.associateBy { it.date })
    val saved = mutableListOf<MealDay>()
    val cleared = mutableListOf<LocalDate>()
    val observedMonths = mutableListOf<YearMonth>()

    val current: Map<LocalDate, MealDay> get() = days.value

    override fun observeMonth(month: YearMonth): Flow<List<MealDay>> {
        observedMonths += month
        return days.map { all -> all.values.filter { YearMonth.from(it.date) == month }.sortedBy { it.date } }
    }

    override suspend fun getDay(date: LocalDate): MealDay? = days.value[date]

    override suspend fun getRange(dates: ClosedRange<LocalDate>): List<MealDay> =
        days.value.values.filter { it.date in dates }.sortedBy { it.date }

    override suspend fun save(day: MealDay) = saveAll(listOf(day))

    override suspend fun saveAll(days: List<MealDay>) {
        saved += days
        this.days.value = this.days.value + days.associateBy { it.date }
    }

    override suspend fun clear(date: LocalDate) {
        cleared += date
        days.value = days.value - date
    }
}
