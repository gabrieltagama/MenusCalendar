package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.model.AutoFillResult
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.RepeatWindow
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
 * Fills every empty dinner of a month (from today on in the current month) or of a given set of
 * dates with one random dinner option (DinnerPicker). Dinners already planned are never modified.
 * Lunches and dinners of the neighbouring days within the user's RepeatWindow are taken into
 * account so the rules also hold across month boundaries.
 * Every filled dinner is saved at the end in a single transaction. Dinners have no heaviness
 * rule, so heavyStreakDays is always 0.
 */
class AutoFillMonthDinnersUseCase @Inject constructor(
    private val dishRepository: DishRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val dinnerPlanRepository: DinnerPlanRepository,
    private val settingsRepository: SettingsRepository,
    private val random: Random,
    private val clock: Clock
) {
    suspend operator fun invoke(month: YearMonth): AutoFillResult = invoke(month.daysToFill(LocalDate.now(clock)))

    suspend operator fun invoke(dates: Collection<LocalDate>): AutoFillResult {
        if (dates.isEmpty()) return AutoFillResult(0, 0, 0, 0)
        val window = settingsRepository.repeatWindow.first()
        val picker = DinnerPicker(dishRepository.getDishes().filter(Dish::isDinnerOption), window, random)
        val range = window.historyRange(dates.min()..dates.max())
        val lunches = mealPlanRepository.getRange(range).associate { it.date to it.menu }
        val dinners = dinnerPlanRepository.getRange(range).associate { it.date to it.dish }.toMutableMap()
        val fill = fillEmptyDays(dates.sorted().filterNot(dinners::containsKey)) { date ->
            picker.pick(date, lunches, dinners)?.let { pick ->
                dinners[date] = pick.dish
                DayPick(DinnerDay(date = date, dish = pick.dish, updatedAt = Instant.now(clock)), pick.repeated)
            }
        }
        dinnerPlanRepository.saveAll(fill.days)
        return fill.result
    }
}

/**
 * Domain service that picks the dinner of one day. From strict to relaxed: never the dish of that
 * day's lunch and none served at lunch or dinner within the RepeatWindow days before or
 * after; then repeating a recent dish; and only when nothing else exists, the lunch dish.
 */
internal class DinnerPicker(private val options: List<Dish>, private val window: RepeatWindow, private val random: Random) {

    fun pick(date: LocalDate, lunches: Map<LocalDate, DailyMenu>, dinners: Map<LocalDate, Dish>): DinnerPick? {
        if (options.isEmpty()) return null
        val lunchIds = lunches[date]?.dishes.orEmpty().map(Dish::id).toSet()
        val nearbyIds = nearbyDishIds(date, lunches, dinners)
        val notLunch = options.filterNot { it.id in lunchIds }
        val dish = notLunch.filterNot { it.id in nearbyIds }.ifEmpty { notLunch }.ifEmpty { options }.random(random)
        return DinnerPick(dish, repeated = dish.id in nearbyIds || dish.id in lunchIds)
    }

    private fun nearbyDishIds(date: LocalDate, lunches: Map<LocalDate, DailyMenu>, dinners: Map<LocalDate, Dish>): Set<DishId> {
        val nearbyDates = window.nearbyDates(date)
        val lunchDishes = nearbyDates.mapNotNull(lunches::get).flatMap(DailyMenu::dishes)
        val dinnerDishes = nearbyDates.mapNotNull(dinners::get)
        return (lunchDishes + dinnerDishes).map(Dish::id).toSet()
    }
}

internal data class DinnerPick(val dish: Dish, val repeated: Boolean)
