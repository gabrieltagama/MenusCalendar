package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.model.RepeatWindow
import com.gabrieltagama.menuplanner.core.domain.port.DinnerPlanRepository
import com.gabrieltagama.menuplanner.core.domain.port.DishRepository
import com.gabrieltagama.menuplanner.core.domain.query.UserPreferencesQueries
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random

/**
 * Dinner autofill (AutoFillPlanUseCase): each empty day gets one random dinner option chosen by
 * DinnerPicker. Dinners have no heaviness rule, so heavyStreakDays is always 0.
 */
class AutoFillMonthDinnersUseCase @Inject constructor(
    dishRepository: DishRepository,
    dinnerPlanRepository: DinnerPlanRepository,
    historyReader: PlanHistoryReader,
    preferences: UserPreferencesQueries,
    private val random: Random,
    clock: Clock
) : AutoFillPlanUseCase<DinnerDay>(Meal.DINNER, dinnerPlanRepository, dishRepository, historyReader, preferences, clock) {

    override fun pickerFor(dishes: List<Dish>, window: RepeatWindow): DayPicker<DinnerDay> {
        val picker = DinnerPicker(dishes.filter(Dish::isDinnerOption), window, random)
        return DayPicker { date, history, at ->
            picker.pick(date, history)?.let { pick ->
                val day = DinnerDay(date = date, dish = pick.dish, updatedAt = at)
                history.record(day)
                DayPick(day, pick.repeated)
            }
        }
    }
}

/**
 * Domain service that picks the dinner of one day. From strict to relaxed: never the dish of that
 * day's lunch and none served at lunch or dinner within the RepeatWindow days before or
 * after; then repeating a recent dish; and only when nothing else exists, the lunch dish.
 */
internal class DinnerPicker(private val options: List<Dish>, private val window: RepeatWindow, private val random: Random) {

    fun pick(date: LocalDate, history: PlanHistory): DinnerPick? {
        if (options.isEmpty()) return null
        val lunchIds = history.lunchOf(date)?.dishes.orEmpty().map(Dish::id).toSet()
        val nearbyIds = history.dishIdsNear(date, window)
        val notLunch = options.filterNot { it.id in lunchIds }
        val dish = notLunch.filterNot { it.id in nearbyIds }.ifEmpty { notLunch }.ifEmpty { options }.random(random)
        return DinnerPick(dish, repeated = dish.id in nearbyIds || dish.id in lunchIds)
    }
}

internal data class DinnerPick(val dish: Dish, val repeated: Boolean)
