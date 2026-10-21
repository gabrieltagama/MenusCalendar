package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.port.MealPlanRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory MealPlanRepository keyed by date, recording saved days and cleared dates. state lets
 * FakePlanQueries read the same plan.
 */
class FakeMealPlanRepository(initial: List<MealDay> = emptyList()) : MealPlanRepository {
    private val days = MutableStateFlow(initial.associateBy { it.date })
    val saved = mutableListOf<MealDay>()
    val cleared = mutableListOf<LocalDate>()

    val current: Map<LocalDate, MealDay> get() = days.value

    val state: StateFlow<Map<LocalDate, MealDay>> = days.asStateFlow()

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

    override suspend fun clearAll(dates: Collection<LocalDate>) {
        cleared += dates
        days.value = days.value - dates.toSet()
    }
}
