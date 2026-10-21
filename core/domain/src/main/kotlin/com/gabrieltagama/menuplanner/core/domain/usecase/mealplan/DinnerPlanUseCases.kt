package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.common.onSuccess
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.plan.PlannedDinner
import com.gabrieltagama.menuplanner.core.domain.port.DinnerPlanRepository
import com.gabrieltagama.menuplanner.core.domain.query.CalendarQueries
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use cases of the monthly dinner calendar. Observing a month is a query that returns read models
 * (PlannedDinner); the other use cases are commands over DinnerDay, most of them the shared plan
 * commands (PlanDaysUseCases) bound to the dinner repository. Saving plans the dinner with
 * DinnerDay.plan, which accepts only dinner options.
 */
class ObserveMonthDinnersUseCase @Inject constructor(private val queries: CalendarQueries) {
    operator fun invoke(month: YearMonth): Flow<List<PlannedDinner>> = queries.observeDinners(month)
}

class GetDinnerDayUseCase @Inject constructor(repository: DinnerPlanRepository) : GetPlanDayUseCase<DinnerDay>(repository)

class SaveDinnerDayUseCase @Inject constructor(
    private val repository: DinnerPlanRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(date: LocalDate, dish: Dish): Outcome<DinnerDay> =
        DinnerDay.plan(date, dish, Instant.now(clock)).onSuccess { repository.save(it) }
}

class ClearDinnerDayUseCase @Inject constructor(repository: DinnerPlanRepository) : ClearPlanDayUseCase<DinnerDay>(repository)

class ClearDinnerDaysUseCase @Inject constructor(repository: DinnerPlanRepository) : ClearPlanDaysUseCase<DinnerDay>(repository)

class RestoreDinnerDaysUseCase @Inject constructor(repository: DinnerPlanRepository) : RestorePlanDaysUseCase<DinnerDay>(repository)
