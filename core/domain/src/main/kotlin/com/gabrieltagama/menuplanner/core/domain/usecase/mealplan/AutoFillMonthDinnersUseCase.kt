package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.model.AutoFillResult
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.port.DinnerPlanRepository
import com.gabrieltagama.menuplanner.core.domain.port.DishRepository
import com.gabrieltagama.menuplanner.core.domain.port.MealPlanRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlin.random.Random

/**
 * Fills every empty dinner of a month with one random dinner option (DinnerPicker). Dinners
 * already planned are never modified. Lunches and dinners of the neighbouring months are taken
 * into account so the rules also hold across month boundaries. Dinners have no heaviness rule,
 * so heavyStreakDays is always 0. Every new dinner is stored in one atomic batch.
 */
class AutoFillMonthDinnersUseCase @Inject constructor(
    private val dishRepository: DishRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val dinnerPlanRepository: DinnerPlanRepository,
    private val random: Random,
    private val clock: Clock
) {
    suspend operator fun invoke(month: YearMonth): AutoFillResult {
        val picker = DinnerPicker(dishRepository.getDishes().filter(Dish::isDinnerOption), random)
        val window = month.minusMonths(1).atDay(1)..month.plusMonths(1).atEndOfMonth()
        val lunches = mealPlanRepository.getRange(window).associate { it.date to it.menu }
        val dinners = dinnerPlanRepository.getRange(window).associate { it.date to it.dish }.toMutableMap()
        val emptyDays = month.days().filterNot(dinners::containsKey)
        val picks = emptyDays.mapNotNull { date -> picker.pick(date, lunches, dinners)?.also { dinners[date] = it.dish }?.let { date to it } }
        val updatedAt = Instant.now(clock)
        dinnerPlanRepository.saveAll(picks.map { (date, pick) -> DinnerDay(date = date, dish = pick.dish, updatedAt = updatedAt) })
        return AutoFillResult(
            filledDays = picks.size,
            repeatedDishDays = picks.count { (_, pick) -> pick.repeated },
            heavyStreakDays = 0,
            unfilledDays = emptyDays.size - picks.size
        )
    }
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
