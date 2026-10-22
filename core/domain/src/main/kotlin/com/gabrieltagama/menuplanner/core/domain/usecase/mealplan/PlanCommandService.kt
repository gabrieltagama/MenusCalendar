package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.model.AutoFillResult
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.LunchDay
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.model.PlannedDay
import com.gabrieltagama.menuplanner.core.domain.port.TransactionRunner
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Application service with the commands of the monthly calendar on the lunch or the dinner plan. DayPlanCommands holds the use
 * cases of one meal, written once for any PlannedDay; LunchPlanCommands and DinnerPlanCommands bind
 * them to each meal, and PlanCommandService picks the one of the requested meal, so callers never
 * branch on it. Every command and its undo runs as one unit of work (TransactionRunner): its reads
 * and writes are kept together or not at all, and no other write lands between them. Deletions and
 * pastes return what changed as an UndoableChange that knows how to undo it.
 */
class PlanCommandService @Inject constructor(
    private val lunches: LunchPlanCommands,
    private val dinners: DinnerPlanCommands,
    private val transaction: TransactionRunner
) {
    suspend fun autoFill(month: YearMonth, meal: Meal): AutoFillResult = transaction.inTransaction { of(meal).autoFill(month) }

    suspend fun autoFill(dates: Set<LocalDate>, meal: Meal): AutoFillResult = transaction.inTransaction { of(meal).autoFill(dates) }

    suspend fun delete(dates: Set<LocalDate>, meal: Meal): UndoableChange = transaction.inTransaction { of(meal).delete(dates) }

    suspend fun paste(dates: Set<LocalDate>, meal: Meal, target: LocalDate): UndoableChange = transaction.inTransaction { of(meal).paste(dates, target) }

    suspend fun undo(change: UndoableChange) = transaction.inTransaction { change.undo() }

    private fun of(meal: Meal): DayPlanCommands<*> = when (meal) {
        Meal.LUNCH -> lunches
        Meal.DINNER -> dinners
    }
}

abstract class DayPlanCommands<D : PlannedDay<D>>(
    private val meal: Meal,
    private val autoFillPlan: AutoFillPlanUseCase<D>,
    private val clearDays: ClearPlanDaysUseCase<D>,
    private val restoreDays: RestorePlanDaysUseCase<D>,
    private val pasteDays: PastePlanDaysUseCase<D>,
    private val undoPasteDays: UndoPastePlanDaysUseCase<D>
) {
    suspend fun autoFill(month: YearMonth): AutoFillResult = autoFillPlan(month)

    suspend fun autoFill(dates: Set<LocalDate>): AutoFillResult = autoFillPlan(dates)

    suspend fun delete(dates: Set<LocalDate>): UndoableChange {
        val removed = clearDays(dates)
        return UndoableChange(view = meal, count = removed.size) { restoreDays(removed) }
    }

    suspend fun paste(dates: Set<LocalDate>, target: LocalDate): UndoableChange {
        val pasted = pasteDays(dates, target)
        return UndoableChange(view = meal, count = pasted.count) { undoPasteDays(pasted) }
    }
}

class LunchPlanCommands @Inject constructor(
    autoFill: AutoFillMonthLunchesUseCase,
    clear: ClearLunchDaysUseCase,
    restore: RestoreLunchDaysUseCase,
    paste: PasteLunchDaysUseCase,
    undoPaste: UndoPasteLunchDaysUseCase
) : DayPlanCommands<LunchDay>(Meal.LUNCH, autoFill, clear, restore, paste, undoPaste)

class DinnerPlanCommands @Inject constructor(
    autoFill: AutoFillMonthDinnersUseCase,
    clear: ClearDinnerDaysUseCase,
    restore: RestoreDinnerDaysUseCase,
    paste: PasteDinnerDaysUseCase,
    undoPaste: UndoPasteDinnerDaysUseCase
) : DayPlanCommands<DinnerDay>(Meal.DINNER, autoFill, clear, restore, paste, undoPaste)

class UndoableChange(val view: Meal, val count: Int, private val undoAction: suspend () -> Unit) {
    suspend fun undo() = undoAction()
}
