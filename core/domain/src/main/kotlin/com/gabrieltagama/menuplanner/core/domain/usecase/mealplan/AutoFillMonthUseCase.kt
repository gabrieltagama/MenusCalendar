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
 * Fills every empty day of a month (or of a given set of dates) with a random menu built by MenuPicker, using only dishes
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
    suspend operator fun invoke(month: YearMonth): AutoFillResult = invoke(month.days())

    suspend operator fun invoke(dates: Collection<LocalDate>): AutoFillResult {
        if (dates.isEmpty()) return AutoFillResult(0, 0, 0, 0)
        val lunchDishes = dishRepository.getDishes().filter { it.mealTime.isServedAtLunch }
        val picker = MenuPicker(lunchDishes, random)
        val history = loadHistory(dates.min()..dates.max())
        val fill = fillEmptyDays(dates.sorted().filterNot(history::containsKey)) { date ->
            picker.pick(date, history)?.let { pick ->
                history[date] = pick.menu
                DayPick(MealDay(date = date, menu = pick.menu, updatedAt = Instant.now(clock)), pick.repeated, pick.breaksHeavyRule)
            }
        }
        mealPlanRepository.saveAll(fill.days)
        return fill.result
    }

    private suspend fun loadHistory(dates: ClosedRange<LocalDate>): MutableMap<LocalDate, DailyMenu> {
        val window = PlanningPolicy.window(dates)
        return mealPlanRepository.getRange(window).associate { it.date to it.menu }.toMutableMap()
    }
}
