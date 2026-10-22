package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.common.onSuccess
import com.gabrieltagama.menuplanner.core.domain.model.AutoFillResult
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.model.PlannedDay
import com.gabrieltagama.menuplanner.core.domain.port.DayPlanRepository
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

/**
 * Commands on the calendar of one meal, written once for any PlannedDay: read and clear one day,
 * autofill, delete several days and paste copied days. delete and paste return what changed as an
 * UndoableChange: undoing a deletion puts the removed days back, and undoing a paste removes the
 * pasted days and restores the ones they replaced. A paste keeps the gaps between the copied days
 * from the target date on (the first copied date lands on the target) and overwrites what was
 * planned there. LunchPlanCommands and DinnerPlanCommands bind it to each meal and add how a day is
 * planned by hand.
 */
abstract class DayPlanCommands<D : PlannedDay<D>> internal constructor(
    val meal: Meal,
    private val repository: DayPlanRepository<D>,
    private val autoFillPlan: AutoFillPlanUseCase<D>,
    protected val clock: Clock
) {
    suspend fun getDay(date: LocalDate): D? = repository.getDay(date)

    suspend fun clearDay(date: LocalDate) = repository.clear(date)

    suspend fun autoFill(month: YearMonth): AutoFillResult = autoFillPlan(month)

    suspend fun autoFill(dates: Set<LocalDate>): AutoFillResult = autoFillPlan(dates)

    suspend fun delete(dates: Set<LocalDate>): UndoableChange {
        val removed = daysAt(dates)
        if (removed.isNotEmpty()) repository.clearAll(removed.map { it.date })
        return UndoableChange(meal = meal, count = removed.size) { repository.saveAll(removed) }
    }

    suspend fun paste(copied: Set<LocalDate>, target: LocalDate): UndoableChange {
        val pasted = pastedDays(copied, target)
        val replaced = daysAt(pasted.map { it.date }.toSet())
        if (pasted.isNotEmpty()) repository.saveAll(pasted)
        return UndoableChange(meal = meal, count = pasted.size) {
            repository.clearAll(pasted.map { it.date })
            repository.saveAll(replaced)
        }
    }

    protected suspend fun persist(planned: Outcome<D>): Outcome<D> = planned.onSuccess { repository.save(it) }

    private suspend fun pastedDays(copied: Set<LocalDate>, target: LocalDate): List<D> {
        if (copied.isEmpty()) return emptyList()
        val start = copied.min()
        val now = clock.instant()
        return daysAt(copied).map { it.movedTo(target.plusDays(it.date.toEpochDay() - start.toEpochDay()), now) }
    }

    private suspend fun daysAt(dates: Set<LocalDate>): List<D> =
        if (dates.isEmpty()) emptyList() else repository.getRange(dates.min()..dates.max()).filter { it.date in dates }
}
