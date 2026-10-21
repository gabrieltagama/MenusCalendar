package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.port.DinnerPlanRepository
import com.gabrieltagama.menuplanner.core.domain.port.MealPlanRepository
import java.time.Clock
import javax.inject.Inject

/**
 * Copy and paste of lunches and dinners: the shared paste and undo commands (PlanDaysUseCases)
 * bound to the repository of each meal.
 */
class PasteMealDaysUseCase @Inject constructor(repository: MealPlanRepository, clock: Clock) : PastePlanDaysUseCase<MealDay>(repository, clock)

class UndoPasteMealDaysUseCase @Inject constructor(repository: MealPlanRepository) : UndoPastePlanDaysUseCase<MealDay>(repository)

class PasteDinnerDaysUseCase @Inject constructor(repository: DinnerPlanRepository, clock: Clock) : PastePlanDaysUseCase<DinnerDay>(repository, clock)

class UndoPasteDinnerDaysUseCase @Inject constructor(repository: DinnerPlanRepository) : UndoPastePlanDaysUseCase<DinnerDay>(repository)
