package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.model.plan.PlannedLunch
import com.gabrieltagama.menuplanner.core.domain.query.CalendarQueries
import com.gabrieltagama.menuplanner.core.domain.repository.MealPlanRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use cases of the monthly calendar. Observing a month is a query that returns read models
 * (PlannedLunch); the other use cases are commands over MealDay. Saving rejects a menu whose dishes are not in the slot of
 * their type (DailyMenu.violation). Clearing several days returns the removed days so they can
 * be restored (undo).
 */
class ObserveMonthPlanUseCase @Inject constructor(private val queries: CalendarQueries) {
    operator fun invoke(month: YearMonth): Flow<List<PlannedLunch>> = queries.observeLunches(month)
}

class GetMealDayUseCase @Inject constructor(private val repository: MealPlanRepository) {
    suspend operator fun invoke(date: LocalDate): MealDay? = repository.getDay(date)
}

class SaveMealDayUseCase @Inject constructor(
    private val repository: MealPlanRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(date: LocalDate, menu: DailyMenu): Outcome<MealDay> {
        menu.violation?.let { return Outcome.Failure(it) }
        val day = MealDay(date = date, menu = menu, updatedAt = Instant.now(clock))
        repository.save(day)
        return Outcome.Success(day)
    }
}

class ClearMealDayUseCase @Inject constructor(private val repository: MealPlanRepository) {
    suspend operator fun invoke(date: LocalDate) = repository.clear(date)
}

class ClearMealDaysUseCase @Inject constructor(private val repository: MealPlanRepository) {
    suspend operator fun invoke(dates: Set<LocalDate>): List<MealDay> {
        if (dates.isEmpty()) return emptyList()
        val removed = repository.getRange(dates.min()..dates.max()).filter { it.date in dates }
        if (removed.isNotEmpty()) repository.clearAll(removed.map(MealDay::date))
        return removed
    }
}

class RestoreMealDaysUseCase @Inject constructor(private val repository: MealPlanRepository) {
    suspend operator fun invoke(days: List<MealDay>) = repository.saveAll(days)
}
