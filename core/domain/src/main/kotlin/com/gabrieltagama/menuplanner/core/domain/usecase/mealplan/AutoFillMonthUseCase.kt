package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.model.AutoFillResult
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.repository.DinnerPlanRepository
import com.gabrieltagama.menuplanner.core.domain.repository.DishRepository
import com.gabrieltagama.menuplanner.core.domain.repository.MealPlanRepository
import com.gabrieltagama.menuplanner.core.domain.repository.SettingsRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.flow.first

/**
 * Fills every empty day of a month (from today on in the current month) or of a given set of dates
 * with a random menu built by MenuPicker, using only dishes served at lunch (MealTime LUNCH or ANY).
 * Days already planned are never modified. Lunches and dinners of the neighbouring days within the
 * user's RepeatWindow are taken into account, so the no-repeat and heavy-streak rules also hold
 * across month boundaries and a lunch avoids the dinners. Every filled day is saved at the end in a
 * single transaction.
 */
class AutoFillMonthUseCase @Inject constructor(
    private val dishRepository: DishRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val dinnerPlanRepository: DinnerPlanRepository,
    private val settingsRepository: SettingsRepository,
    private val random: Random,
    private val clock: Clock
) {
    suspend operator fun invoke(month: YearMonth): AutoFillResult = invoke(month.daysToFill(LocalDate.now(clock)))

    suspend operator fun invoke(dates: Collection<LocalDate>): AutoFillResult {
        if (dates.isEmpty()) return AutoFillResult.EMPTY
        val window = settingsRepository.repeatWindow.first()
        val lunchDishes = dishRepository.getDishes().filter(Dish::isLunchOption)
        val picker = MenuPicker(lunchDishes, window, random)
        val range = window.historyRange(dates.min()..dates.max())
        val history = mealPlanRepository.getRange(range).associate { it.date to it.menu }.toMutableMap()
        val dinners = dinnerPlanRepository.getRange(range).associate { it.date to it.dish }
        val now = Instant.now(clock)
        val fill = fillEmptyDays(dates.sorted().filterNot(history::containsKey)) { date ->
            picker.pick(date, history, dinners)?.let { pick ->
                history[date] = pick.menu
                DayPick(MealDay(date = date, menu = pick.menu, updatedAt = now), pick.repeated, pick.breaksHeavyRule)
            }
        }
        mealPlanRepository.saveAll(fill.days)
        return fill.result
    }
}
