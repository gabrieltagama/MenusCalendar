package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.model.plan.PlannedDinner
import com.gabrieltagama.menuplanner.core.domain.model.plan.PlannedLunch
import com.gabrieltagama.menuplanner.core.domain.model.plan.toPlannedDinner
import com.gabrieltagama.menuplanner.core.domain.model.plan.toPlannedLunch
import com.gabrieltagama.menuplanner.core.domain.query.CalendarQueries
import com.gabrieltagama.menuplanner.core.domain.query.ShoppingListQueries
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * In-memory read side over the fake plan repositories, so commands written through them are seen
 * by the queries, as with Room. Lines keep the order of the real query: date, menu slot, position.
 */
class FakePlanQueries(
    private val lunchPlan: FakeLunchPlanRepository = FakeLunchPlanRepository(),
    private val dinnerPlan: FakeDinnerPlanRepository = FakeDinnerPlanRepository()
) : CalendarQueries, ShoppingListQueries {

    override fun observeLunches(month: YearMonth): Flow<List<PlannedLunch>> =
        lunchPlan.state.map { days -> days.values.filter { YearMonth.from(it.date) == month }.sortedBy { it.date }.map { it.toPlannedLunch() } }

    override fun observeDinners(month: YearMonth): Flow<List<PlannedDinner>> =
        dinnerPlan.state.map { days -> days.values.filter { YearMonth.from(it.date) == month }.sortedBy { it.date }.map { it.toPlannedDinner() } }

    override fun observeIngredientLines(dates: ClosedRange<LocalDate>, meal: Meal): Flow<List<Ingredient>> = when (meal) {
        Meal.LUNCH -> lunchPlan.state.map { days -> days.values.filter { it.date in dates }.sortedBy { it.date }.flatMap { it.menu.dishes }.flatMap { it.ingredients } }
        Meal.DINNER -> dinnerPlan.state.map { days -> days.values.filter { it.date in dates }.sortedBy { it.date }.flatMap { it.dish.ingredients } }
    }
}
