package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.model.AutoFillResult
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.model.PlanningPolicy
import com.gabrieltagama.menuplanner.core.domain.repository.DishRepository
import com.gabrieltagama.menuplanner.core.domain.repository.MealPlanRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlin.random.Random

/**
 * Fills every empty day of a month with a random menu built by MenuPicker, using only dishes
 * served at lunch (MealTime LUNCH or ANY). Days already planned are never modified; the days of
 * the previous and next months within PlanningPolicy.RECENT_DAYS are taken into account so the
 * no-repeat and heavy-streak rules also hold across both month boundaries. Every filled day is
 * saved at the end in a single transaction.
 */
class AutoFillMonthUseCase @Inject constructor(
    private val dishRepository: DishRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val random: Random,
    private val clock: Clock
) {
    suspend operator fun invoke(month: YearMonth): AutoFillResult {
        val lunchDishes = dishRepository.getDishes().filter { it.mealTime.isServedAtLunch }
        val picker = MenuPicker(lunchDishes, random)
        val history = loadHistory(month)
        val emptyDays = month.days().filterNot(history::containsKey)
        val picks = mutableListOf<MenuPick>()
        val filled = mutableListOf<MealDay>()
        for (date in emptyDays) {
            val pick = picker.pick(date, history) ?: continue
            history[date] = pick.menu
            filled += MealDay(date = date, menu = pick.menu, updatedAt = Instant.now(clock))
            picks += pick
        }
        mealPlanRepository.saveAll(filled)
        return AutoFillResult(
            filledDays = picks.size,
            repeatedDishDays = picks.count(MenuPick::repeated),
            heavyStreakDays = picks.count(MenuPick::breaksHeavyRule),
            unfilledDays = emptyDays.size - picks.size
        )
    }

    private suspend fun loadHistory(month: YearMonth): MutableMap<LocalDate, DailyMenu> {
        val window = PlanningPolicy.window(month.atDay(1)..month.atEndOfMonth())
        return mealPlanRepository.getRange(window).associate { it.date to it.menu }.toMutableMap()
    }

    private fun YearMonth.days(): List<LocalDate> = (1..lengthOfMonth()).map(::atDay)
}
