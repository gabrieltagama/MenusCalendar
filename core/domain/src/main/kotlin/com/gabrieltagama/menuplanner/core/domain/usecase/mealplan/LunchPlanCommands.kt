package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.LunchDay
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.port.LunchPlanRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Commands of the lunch calendar (DayPlanCommands). save plans the day with LunchDay.plan, which
 * rejects a menu whose dishes are not in the slot of their type.
 */
class LunchPlanCommands @Inject constructor(
    repository: LunchPlanRepository,
    autoFill: AutoFillMonthLunchesUseCase,
    clock: Clock
) : DayPlanCommands<LunchDay>(Meal.LUNCH, repository, autoFill, clock) {

    suspend fun save(date: LocalDate, menu: DailyMenu): Outcome<LunchDay> = persist(LunchDay.plan(date, menu, clock.instant()))
}
