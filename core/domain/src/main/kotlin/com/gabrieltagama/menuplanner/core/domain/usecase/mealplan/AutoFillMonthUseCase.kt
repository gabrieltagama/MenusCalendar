package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.model.AutoFillResult
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
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
 * served at lunch (MealTime LUNCH or ANY). Days already planned are never modified; the last
 * days of the previous month are taken into account so the no-repeat and heavy-streak rules also
 * hold across the month boundary. Every new day is stored in one atomic batch, so the month is
 * never left half filled.
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
        val picks = emptyDays.mapNotNull { date -> picker.pick(date, history)?.also { history[date] = it.menu }?.let { date to it } }
        val updatedAt = Instant.now(clock)
        mealPlanRepository.saveAll(picks.map { (date, pick) -> MealDay(date = date, menu = pick.menu, updatedAt = updatedAt) })
        return AutoFillResult(
            filledDays = picks.size,
            repeatedDishDays = picks.count { (_, pick) -> pick.repeated },
            heavyStreakDays = picks.count { (_, pick) -> pick.breaksHeavyRule },
            unfilledDays = emptyDays.size - picks.size
        )
    }

    private suspend fun loadHistory(month: YearMonth): MutableMap<LocalDate, DailyMenu> =
        mealPlanRepository.getRange(month.atDay(1).minusDays(MenuPicker.RECENT_DAYS)..month.atEndOfMonth())
            .associate { it.date to it.menu }
            .toMutableMap()
}
