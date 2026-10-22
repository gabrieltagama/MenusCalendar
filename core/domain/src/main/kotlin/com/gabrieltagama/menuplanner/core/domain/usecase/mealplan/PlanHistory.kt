package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.LunchDay
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.model.PlanningPolicy
import com.gabrieltagama.menuplanner.core.domain.model.RepeatWindow
import com.gabrieltagama.menuplanner.core.domain.port.DinnerPlanRepository
import com.gabrieltagama.menuplanner.core.domain.port.LunchPlanRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Lunches and dinners around the dates an autofill run plans, read once and updated with every
 * day the run picks, so each pick sees the ones before it. It answers the questions both pickers
 * ask: which dishes were served at lunch or dinner within the RepeatWindow of a date and whether
 * the day before or after is a heavy lunch day (PlanningPolicy).
 */
internal class PlanHistory(lunches: Map<LocalDate, DailyMenu>, dinners: Map<LocalDate, Dish>) {

    private val lunches = lunches.toMutableMap()
    private val dinners = dinners.toMutableMap()

    fun lunchOf(date: LocalDate): DailyMenu? = lunches[date]

    fun dinnerOf(date: LocalDate): Dish? = dinners[date]

    fun isPlanned(date: LocalDate, meal: Meal): Boolean = when (meal) {
        Meal.LUNCH -> date in lunches
        Meal.DINNER -> date in dinners
    }

    fun record(day: LunchDay) {
        lunches[day.date] = day.menu
    }

    fun record(day: DinnerDay) {
        dinners[day.date] = day.dish
    }

    fun dishIdsNear(date: LocalDate, window: RepeatWindow): Set<DishId> {
        val nearbyDates = window.nearbyDates(date)
        val lunchDishes = nearbyDates.mapNotNull(lunches::get).flatMap(DailyMenu::dishes)
        val dinnerDishes = nearbyDates.mapNotNull(dinners::get)
        return (lunchDishes + dinnerDishes).map(Dish::id).toSet()
    }

    fun isNextToHeavyDay(date: LocalDate): Boolean =
        PlanningPolicy.isHeavyDay(lunches[date.minusDays(1)]) || PlanningPolicy.isHeavyDay(lunches[date.plusDays(1)])
}

/**
 * Reads the lunches and dinners of a range of dates into a PlanHistory, so the autofill depends on
 * one reader instead of both calendar repositories.
 */
class PlanHistoryReader @Inject constructor(
    private val lunchPlanRepository: LunchPlanRepository,
    private val dinnerPlanRepository: DinnerPlanRepository
) {
    internal suspend fun read(dates: ClosedRange<LocalDate>): PlanHistory = PlanHistory(
        lunches = lunchPlanRepository.getRange(dates).associate { it.date to it.menu },
        dinners = dinnerPlanRepository.getRange(dates).associate { it.date to it.dish }
    )
}
