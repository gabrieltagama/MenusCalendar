package com.gabrieltagama.menuplanner.feature.calendar.month

import com.gabrieltagama.menuplanner.core.domain.model.AutoFillResult
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.model.PlannedDay
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.AutoFillMonthDinnersUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.AutoFillMonthUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.AutoFillPlanUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ClearDinnerDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ClearMealDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ClearPlanDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.PasteDinnerDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.PasteMealDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.PastePlanDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.RestoreDinnerDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.RestoreMealDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.RestorePlanDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.UndoPasteDinnerDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.UndoPasteMealDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.UndoPastePlanDaysUseCase
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Commands of the monthly calendar on the lunch or the dinner plan. MealPlanCommands holds the use
 * cases of one meal, written once for any PlannedDay; LunchPlanCommands and DinnerPlanCommands bind
 * them to each meal, and PlanCommands picks the one of the active view, so CalendarViewModel never
 * branches on the meal. Deletions and pastes return what changed (PlanDeletion, PlanPaste) together
 * with how to undo it.
 */
class PlanCommands @Inject constructor(
    private val lunches: LunchPlanCommands,
    private val dinners: DinnerPlanCommands
) {
    suspend fun autoFill(month: YearMonth, meal: Meal): AutoFillResult = of(meal).autoFill(month)

    suspend fun autoFill(dates: Set<LocalDate>, meal: Meal): AutoFillResult = of(meal).autoFill(dates)

    suspend fun delete(dates: Set<LocalDate>, meal: Meal): PlanDeletion = of(meal).delete(dates)

    suspend fun paste(clipboard: PlanClipboard, target: LocalDate): PlanPaste = of(clipboard.view).paste(clipboard.dates, target)

    suspend fun undo(deletion: PlanDeletion) = deletion.undo()

    suspend fun undo(paste: PlanPaste) = paste.undo()

    private fun of(meal: Meal): MealPlanCommands<*> = when (meal) {
        Meal.LUNCH -> lunches
        Meal.DINNER -> dinners
    }
}

abstract class MealPlanCommands<D : PlannedDay<D>>(
    private val meal: Meal,
    private val autoFillPlan: AutoFillPlanUseCase<D>,
    private val clearDays: ClearPlanDaysUseCase<D>,
    private val restoreDays: RestorePlanDaysUseCase<D>,
    private val pasteDays: PastePlanDaysUseCase<D>,
    private val undoPasteDays: UndoPastePlanDaysUseCase<D>
) {
    suspend fun autoFill(month: YearMonth): AutoFillResult = autoFillPlan(month)

    suspend fun autoFill(dates: Set<LocalDate>): AutoFillResult = autoFillPlan(dates)

    suspend fun delete(dates: Set<LocalDate>): PlanDeletion {
        val removed = clearDays(dates)
        return PlanDeletion(view = meal, count = removed.size) { restoreDays(removed) }
    }

    suspend fun paste(dates: Set<LocalDate>, target: LocalDate): PlanPaste {
        val pasted = pasteDays(dates, target)
        return PlanPaste(view = meal, count = pasted.count) { undoPasteDays(pasted) }
    }
}

class LunchPlanCommands @Inject constructor(
    autoFill: AutoFillMonthUseCase,
    clear: ClearMealDaysUseCase,
    restore: RestoreMealDaysUseCase,
    paste: PasteMealDaysUseCase,
    undoPaste: UndoPasteMealDaysUseCase
) : MealPlanCommands<MealDay>(Meal.LUNCH, autoFill, clear, restore, paste, undoPaste)

class DinnerPlanCommands @Inject constructor(
    autoFill: AutoFillMonthDinnersUseCase,
    clear: ClearDinnerDaysUseCase,
    restore: RestoreDinnerDaysUseCase,
    paste: PasteDinnerDaysUseCase,
    undoPaste: UndoPasteDinnerDaysUseCase
) : MealPlanCommands<DinnerDay>(Meal.DINNER, autoFill, clear, restore, paste, undoPaste)

class PlanDeletion(val view: Meal, val count: Int, private val undoAction: suspend () -> Unit) {
    suspend fun undo() = undoAction()
}

class PlanPaste(val view: Meal, val count: Int, private val undoAction: suspend () -> Unit) {
    suspend fun undo() = undoAction()
}
