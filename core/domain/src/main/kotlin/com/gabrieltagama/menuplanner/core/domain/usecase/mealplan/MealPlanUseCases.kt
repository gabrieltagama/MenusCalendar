package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.port.MealPlanRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use cases of the monthly calendar. Saving checks that every dish sits in a slot that matches
 * its type (starter, main, single or dessert).
 */
class ObserveMonthPlanUseCase @Inject constructor(private val repository: MealPlanRepository) {
    operator fun invoke(month: YearMonth): Flow<List<MealDay>> = repository.observeMonth(month)
}

class GetMealDayUseCase @Inject constructor(private val repository: MealPlanRepository) {
    suspend operator fun invoke(date: LocalDate): MealDay? = repository.getDay(date)
}

class SaveMealDayUseCase @Inject constructor(
    private val repository: MealPlanRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(date: LocalDate, menu: DailyMenu): Outcome<MealDay> {
        validate(menu)?.let { return Outcome.Failure(it) }
        val day = MealDay(date = date, menu = menu, updatedAt = Instant.now(clock))
        repository.save(day)
        return Outcome.Success(day)
    }

    private fun validate(menu: DailyMenu): DomainError? =
        menu.misplacedSlot?.let { DomainError.WrongDishType(it, it.dishType) }
}

class ClearMealDayUseCase @Inject constructor(private val repository: MealPlanRepository) {
    suspend operator fun invoke(date: LocalDate) = repository.clear(date)
}
