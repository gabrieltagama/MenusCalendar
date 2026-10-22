package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.LunchDay
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.model.RepeatWindow
import com.gabrieltagama.menuplanner.core.domain.port.DishRepository
import com.gabrieltagama.menuplanner.core.domain.port.LunchPlanRepository
import com.gabrieltagama.menuplanner.core.domain.port.UserPreferencesRepository
import java.time.Clock
import javax.inject.Inject
import kotlin.random.Random

/**
 * Lunch autofill (AutoFillPlanUseCase): each empty day gets a random menu built by MenuPicker from
 * the lunch options (Dish.isLunchOption), so the no-repeat and heavy-streak rules hold and a lunch
 * avoids the dinners around it.
 */
class AutoFillMonthLunchesUseCase @Inject constructor(
    dishRepository: DishRepository,
    lunchPlanRepository: LunchPlanRepository,
    historyReader: PlanHistoryReader,
    preferencesRepository: UserPreferencesRepository,
    private val random: Random,
    clock: Clock
) : AutoFillPlanUseCase<LunchDay>(Meal.LUNCH, lunchPlanRepository, dishRepository, historyReader, preferencesRepository, clock) {

    override fun pickerFor(dishes: List<Dish>, window: RepeatWindow): DayPicker<LunchDay> {
        val picker = MenuPicker(dishes.filter(Dish::isLunchOption), window, random)
        return DayPicker { date, history, at ->
            picker.pick(date, history)?.let { pick ->
                val day = LunchDay(date = date, menu = pick.menu, updatedAt = at)
                history.record(day)
                DayPick(day, pick.repeated, pick.breaksHeavyRule)
            }
        }
    }
}
