package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.common.onSuccess
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.model.plan.PlannedLunch
import com.gabrieltagama.menuplanner.core.domain.port.MealPlanRepository
import com.gabrieltagama.menuplanner.core.domain.query.CalendarQueries
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use cases of the monthly lunch calendar. Observing a month is a query that returns read models
 * (PlannedLunch); the other use cases are commands over MealDay, most of them the shared plan
 * commands (PlanDaysUseCases) bound to the lunch repository. Saving plans the day with MealDay.plan,
 * which rejects a menu whose dishes are not in the slot of their type.
 */
class ObserveMonthPlanUseCase @Inject constructor(private val queries: CalendarQueries) {
    operator fun invoke(month: YearMonth): Flow<List<PlannedLunch>> = queries.observeLunches(month)
}

class GetMealDayUseCase @Inject constructor(repository: MealPlanRepository) : GetPlanDayUseCase<MealDay>(repository)

class SaveMealDayUseCase @Inject constructor(
    private val repository: MealPlanRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(date: LocalDate, menu: DailyMenu): Outcome<MealDay> =
        MealDay.plan(date, menu, Instant.now(clock)).onSuccess { repository.save(it) }
}

class ClearMealDayUseCase @Inject constructor(repository: MealPlanRepository) : ClearPlanDayUseCase<MealDay>(repository)

class ClearMealDaysUseCase @Inject constructor(repository: MealPlanRepository) : ClearPlanDaysUseCase<MealDay>(repository)

class RestoreMealDaysUseCase @Inject constructor(repository: MealPlanRepository) : RestorePlanDaysUseCase<MealDay>(repository)
