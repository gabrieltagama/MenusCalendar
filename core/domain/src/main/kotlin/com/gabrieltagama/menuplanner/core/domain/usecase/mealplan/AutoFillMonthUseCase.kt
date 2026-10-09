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
import kotlinx.coroutines.flow.first

/**
 * Fills every empty day of a month with a random menu built by MenuPicker. Days already planned
 * are never modified; the last days of the previous month are taken into account so the
 * no-repeat and heavy-streak rules also hold across the month boundary.
 */
class AutoFillMonthUseCase @Inject constructor(
    private val dishRepository: DishRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val random: Random,
    private val clock: Clock
) {
    suspend operator fun invoke(month: YearMonth): AutoFillResult {
        val picker = MenuPicker(dishRepository.observeDishes().first(), random)
        val history = loadHistory(month)
        val emptyDays = month.days().filterNot(history::containsKey)
        val picks = mutableListOf<MenuPick>()
        for (date in emptyDays) {
            val pick = picker.pick(date, history) ?: continue
            history[date] = pick.menu
            mealPlanRepository.save(MealDay(date = date, menu = pick.menu, updatedAt = Instant.now(clock)))
            picks += pick
        }
        return AutoFillResult(
            filledDays = picks.size,
            repeatedDishDays = picks.count(MenuPick::repeated),
            heavyStreakDays = picks.count(MenuPick::breaksHeavyRule),
            unfilledDays = emptyDays.size - picks.size
        )
    }

    private suspend fun loadHistory(month: YearMonth): MutableMap<LocalDate, DailyMenu> {
        val windowStart = month.atDay(1).minusDays(MenuPicker.RECENT_DAYS)
        val previous = mealPlanRepository.observeMonth(month.minusMonths(1)).first().filter { !it.date.isBefore(windowStart) }
        val current = mealPlanRepository.observeMonth(month).first()
        return (previous + current).associate { it.date to it.menu }.toMutableMap()
    }

    private fun YearMonth.days(): List<LocalDate> = (1..lengthOfMonth()).map(::atDay)
}
