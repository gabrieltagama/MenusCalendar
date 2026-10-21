package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.model.LunchDay
import com.gabrieltagama.menuplanner.core.domain.port.LunchPlanRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory LunchPlanRepository keyed by date, recording saved days and cleared dates. state lets
 * FakePlanQueries read the same plan.
 */
class FakeLunchPlanRepository(initial: List<LunchDay> = emptyList()) : LunchPlanRepository {
    private val days = MutableStateFlow(initial.associateBy { it.date })
    val saved = mutableListOf<LunchDay>()
    val cleared = mutableListOf<LocalDate>()

    val current: Map<LocalDate, LunchDay> get() = days.value

    val state: StateFlow<Map<LocalDate, LunchDay>> = days.asStateFlow()

    override suspend fun getDay(date: LocalDate): LunchDay? = days.value[date]

    override suspend fun getRange(dates: ClosedRange<LocalDate>): List<LunchDay> =
        days.value.values.filter { it.date in dates }.sortedBy { it.date }

    override suspend fun save(day: LunchDay) = saveAll(listOf(day))

    override suspend fun saveAll(days: List<LunchDay>) {
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
