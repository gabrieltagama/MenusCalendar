package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.plan.PlannedDinner
import com.gabrieltagama.menuplanner.core.domain.query.CalendarQueries
import com.gabrieltagama.menuplanner.core.domain.repository.DinnerPlanRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use cases of the monthly dinner calendar. Observing a month is a query that returns read
 * models (PlannedDinner); the other use cases are commands over DinnerDay. Saving accepts only dishes served at dinner (DINNER
 * or ANY) that are not desserts, because a dinner is a single dish. Clearing several dinners
 * returns the removed ones so they can be restored (undo).
 */
class ObserveMonthDinnersUseCase @Inject constructor(private val queries: CalendarQueries) {
    operator fun invoke(month: YearMonth): Flow<List<PlannedDinner>> = queries.observeDinners(month)
}

class GetDinnerDayUseCase @Inject constructor(private val repository: DinnerPlanRepository) {
    suspend operator fun invoke(date: LocalDate): DinnerDay? = repository.getDay(date)
}

class SaveDinnerDayUseCase @Inject constructor(
    private val repository: DinnerPlanRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(date: LocalDate, dish: Dish): Outcome<DinnerDay> {
        if (!dish.isDinnerOption) return Outcome.Failure(DomainError.NotADinnerDish)
        val day = DinnerDay(date = date, dish = dish, updatedAt = Instant.now(clock))
        repository.save(day)
        return Outcome.Success(day)
    }
}

class ClearDinnerDayUseCase @Inject constructor(private val repository: DinnerPlanRepository) {
    suspend operator fun invoke(date: LocalDate) = repository.clear(date)
}

class ClearDinnerDaysUseCase @Inject constructor(private val repository: DinnerPlanRepository) {
    suspend operator fun invoke(dates: Set<LocalDate>): List<DinnerDay> {
        if (dates.isEmpty()) return emptyList()
        val removed = repository.getRange(dates.min()..dates.max()).filter { it.date in dates }
        if (removed.isNotEmpty()) repository.clearAll(removed.map(DinnerDay::date))
        return removed
    }
}

class RestoreDinnerDaysUseCase @Inject constructor(private val repository: DinnerPlanRepository) {
    suspend operator fun invoke(days: List<DinnerDay>) = repository.saveAll(days)
}
