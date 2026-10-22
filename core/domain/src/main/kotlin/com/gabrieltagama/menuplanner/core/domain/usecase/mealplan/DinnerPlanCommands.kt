package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.port.DinnerPlanRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Commands of the dinner calendar (DayPlanCommands). save plans the dinner with DinnerDay.plan,
 * which accepts only dinner options.
 */
class DinnerPlanCommands @Inject constructor(
    repository: DinnerPlanRepository,
    autoFill: AutoFillMonthDinnersUseCase,
    clock: Clock
) : DayPlanCommands<DinnerDay>(Meal.DINNER, repository, autoFill, clock) {

    suspend fun save(date: LocalDate, dish: Dish): Outcome<DinnerDay> = persist(DinnerDay.plan(date, dish, clock.instant()))
}
