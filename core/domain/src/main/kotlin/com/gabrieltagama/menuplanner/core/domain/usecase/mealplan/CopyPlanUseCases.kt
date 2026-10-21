package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.LunchDay
import com.gabrieltagama.menuplanner.core.domain.port.DinnerPlanRepository
import com.gabrieltagama.menuplanner.core.domain.port.LunchPlanRepository
import java.time.Clock
import javax.inject.Inject

/**
 * Copy and paste of lunches and dinners: the shared paste and undo commands (PlanDaysUseCases)
 * bound to the repository of each meal.
 */
class PasteLunchDaysUseCase @Inject constructor(repository: LunchPlanRepository, clock: Clock) : PastePlanDaysUseCase<LunchDay>(repository, clock)

class UndoPasteLunchDaysUseCase @Inject constructor(repository: LunchPlanRepository) : UndoPastePlanDaysUseCase<LunchDay>(repository)

class PasteDinnerDaysUseCase @Inject constructor(repository: DinnerPlanRepository, clock: Clock) : PastePlanDaysUseCase<DinnerDay>(repository, clock)

class UndoPasteDinnerDaysUseCase @Inject constructor(repository: DinnerPlanRepository) : UndoPastePlanDaysUseCase<DinnerDay>(repository)
