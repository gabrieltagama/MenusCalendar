package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.common.onSuccess
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.LunchDay
import com.gabrieltagama.menuplanner.core.domain.model.plan.PlannedLunch
import com.gabrieltagama.menuplanner.core.domain.port.LunchPlanRepository
import com.gabrieltagama.menuplanner.core.domain.query.CalendarQueries
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use cases of the monthly lunch calendar. Observing a month is a query that returns read models
 * (PlannedLunch); the other use cases are commands over LunchDay, most of them the shared plan
 * commands (PlanDaysUseCases) bound to the lunch repository. Saving plans the day with LunchDay.plan,
 * which rejects a menu whose dishes are not in the slot of their type.
 */
class ObserveMonthLunchesUseCase @Inject constructor(private val queries: CalendarQueries) {
    operator fun invoke(month: YearMonth): Flow<List<PlannedLunch>> = queries.observeLunches(month)
}

class GetLunchDayUseCase @Inject constructor(repository: LunchPlanRepository) : GetPlanDayUseCase<LunchDay>(repository)

class SaveLunchDayUseCase @Inject constructor(
    private val repository: LunchPlanRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(date: LocalDate, menu: DailyMenu): Outcome<LunchDay> =
        LunchDay.plan(date, menu, Instant.now(clock)).onSuccess { repository.save(it) }
}

class ClearLunchDayUseCase @Inject constructor(repository: LunchPlanRepository) : ClearPlanDayUseCase<LunchDay>(repository)

class ClearLunchDaysUseCase @Inject constructor(repository: LunchPlanRepository) : ClearPlanDaysUseCase<LunchDay>(repository)

class RestoreLunchDaysUseCase @Inject constructor(repository: LunchPlanRepository) : RestorePlanDaysUseCase<LunchDay>(repository)
