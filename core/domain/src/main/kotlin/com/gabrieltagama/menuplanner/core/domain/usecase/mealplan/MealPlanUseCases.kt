package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.MenuSlot
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.repository.MealPlanRepository
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
        slotsOf(menu).firstNotNullOfOrNull { (slot, dish) -> dish?.let { checkSlot(slot, it) } }

    private fun slotsOf(menu: DailyMenu): List<Pair<MenuSlot, Dish?>> = when (menu) {
        is DailyMenu.Courses -> listOf(MenuSlot.STARTER to menu.starter, MenuSlot.MAIN to menu.main, MenuSlot.DESSERT to menu.dessert)
        is DailyMenu.Single -> listOf(MenuSlot.SINGLE to menu.single, MenuSlot.DESSERT to menu.dessert)
    }

    private fun checkSlot(slot: MenuSlot, dish: Dish): DomainError? {
        val expected = slot.expectedType()
        return if (dish.type == expected) null else DomainError.WrongDishType(slot, expected)
    }

    private fun MenuSlot.expectedType(): DishType = when (this) {
        MenuSlot.STARTER -> DishType.STARTER
        MenuSlot.MAIN -> DishType.MAIN
        MenuSlot.SINGLE -> DishType.SINGLE
        MenuSlot.DESSERT -> DishType.DESSERT
    }
}

class ClearMealDayUseCase @Inject constructor(private val repository: MealPlanRepository) {
    suspend operator fun invoke(date: LocalDate) = repository.clear(date)
}
