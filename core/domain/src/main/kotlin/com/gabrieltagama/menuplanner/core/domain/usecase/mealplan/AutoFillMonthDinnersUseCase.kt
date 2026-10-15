package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.model.AutoFillResult
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.repository.DinnerPlanRepository
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
 * Fills every empty dinner of a month with one random dinner option (DinnerPicker). Dinners
 * already planned are never modified. Lunches and dinners of the neighbouring months are taken
 * into account so the rules also hold across month boundaries. Dinners have no heaviness rule,
 * so heavyStreakDays is always 0.
 */
class AutoFillMonthDinnersUseCase @Inject constructor(
    private val dishRepository: DishRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val dinnerPlanRepository: DinnerPlanRepository,
    private val random: Random,
    private val clock: Clock
) {
    suspend operator fun invoke(month: YearMonth): AutoFillResult {
        val picker = DinnerPicker(dishRepository.observeDishes().first().filter(Dish::isDinnerOption), random)
        val lunches = month.window().flatMap { mealPlanRepository.observeMonth(it).first() }.associate { it.date to it.menu }
        val dinners = month.window().flatMap { dinnerPlanRepository.observeMonth(it).first() }.associate { it.date to it.dish }.toMutableMap()
        val emptyDays = month.days().filterNot(dinners::containsKey)
        val picks = mutableListOf<DinnerPick>()
        for (date in emptyDays) {
            val pick = picker.pick(date, lunches, dinners) ?: continue
            dinners[date] = pick.dish
            dinnerPlanRepository.save(DinnerDay(date = date, dish = pick.dish, updatedAt = Instant.now(clock)))
            picks += pick
        }
        return AutoFillResult(
            filledDays = picks.size,
            repeatedDishDays = picks.count(DinnerPick::repeated),
            heavyStreakDays = 0,
            unfilledDays = emptyDays.size - picks.size
        )
    }

    private fun YearMonth.window(): List<YearMonth> = listOf(minusMonths(1), this, plusMonths(1))

    private fun YearMonth.days(): List<LocalDate> = (1..lengthOfMonth()).map(::atDay)
}

/**
 * Domain service that picks the dinner of one day. From strict to relaxed: never the dish of that
 * day's lunch and none served at lunch or dinner within MenuPicker.RECENT_DAYS days before or
 * after; then repeating a recent dish; and only when nothing else exists, the lunch dish.
 */
internal class DinnerPicker(private val options: List<Dish>, private val random: Random) {

    fun pick(date: LocalDate, lunches: Map<LocalDate, DailyMenu>, dinners: Map<LocalDate, Dish>): DinnerPick? {
        if (options.isEmpty()) return null
        val lunchIds = lunches[date]?.dishes.orEmpty().map(Dish::id).toSet()
        val nearbyIds = nearbyDishIds(date, lunches, dinners)
        val notLunch = options.filterNot { it.id in lunchIds }
        val dish = notLunch.filterNot { it.id in nearbyIds }.ifEmpty { notLunch }.ifEmpty { options }.random(random)
        return DinnerPick(dish, repeated = dish.id in nearbyIds || dish.id in lunchIds)
    }

    private fun nearbyDishIds(date: LocalDate, lunches: Map<LocalDate, DailyMenu>, dinners: Map<LocalDate, Dish>): Set<String> {
        val nearbyDates = (1..MenuPicker.RECENT_DAYS).flatMap { listOf(date.minusDays(it), date.plusDays(it)) }
        val lunchDishes = nearbyDates.mapNotNull(lunches::get).flatMap(DailyMenu::dishes)
        val dinnerDishes = nearbyDates.mapNotNull(dinners::get)
        return (lunchDishes + dinnerDishes).map(Dish::id).toSet()
    }
}

internal data class DinnerPick(val dish: Dish, val repeated: Boolean)
