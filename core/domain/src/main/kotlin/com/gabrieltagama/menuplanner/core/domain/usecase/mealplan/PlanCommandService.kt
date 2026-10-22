package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.model.AutoFillResult
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.port.TransactionRunner
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Application service with the commands of the monthly calendar on the lunch or the dinner plan: it
 * picks the DayPlanCommands of the requested meal, so callers never branch on it. Every command and
 * its undo runs as one unit of work (TransactionRunner): its reads and writes are kept together or
 * not at all, and no other write lands between them.
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
