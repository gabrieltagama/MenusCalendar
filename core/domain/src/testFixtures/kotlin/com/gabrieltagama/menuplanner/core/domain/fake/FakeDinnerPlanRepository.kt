package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.repository.DinnerPlanRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory DinnerPlanRepository keyed by date, recording saved days and cleared dates. state lets
 * FakePlanQueries read the same plan.
 */
class FakeDinnerPlanRepository(initial: List<DinnerDay> = emptyList()) : DinnerPlanRepository {
    private val days = MutableStateFlow(initial.associateBy { it.date })
    val saved = mutableListOf<DinnerDay>()
    val cleared = mutableListOf<LocalDate>()

    val current: Map<LocalDate, DinnerDay> get() = days.value

    val state: StateFlow<Map<LocalDate, DinnerDay>> = days.asStateFlow()

    override suspend fun getDay(date: LocalDate): DinnerDay? = days.value[date]

    override suspend fun getRange(dates: ClosedRange<LocalDate>): List<DinnerDay> =
        days.value.values.filter { it.date in dates }.sortedBy { it.date }

    override suspend fun save(day: DinnerDay) = saveAll(listOf(day))

    override suspend fun saveAll(days: List<DinnerDay>) {
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
