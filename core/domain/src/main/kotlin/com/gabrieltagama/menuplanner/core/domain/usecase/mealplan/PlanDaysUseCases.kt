package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.model.PastedDays
import com.gabrieltagama.menuplanner.core.domain.model.PlannedDay
import com.gabrieltagama.menuplanner.core.domain.model.shifted
import com.gabrieltagama.menuplanner.core.domain.port.DayPlanRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/**
 * Commands shared by the lunch and dinner calendars, written once for any PlannedDay; each meal
 * extends them with its own repository (MealPlanUseCases, DinnerPlanUseCases, CopyPlanUseCases).
 * Clearing several days returns the removed ones so they can be restored (undo). A paste writes the
 * planned days among the copied dates from the target date on, keeping the gaps between them (the
 * first copied date lands on the target), overwrites what was planned there and returns both, so
 * undoing removes the pasted days and puts back the replaced ones.
 */
abstract class GetPlanDayUseCase<D : PlannedDay<D>>(private val repository: DayPlanRepository<D>) {
    suspend operator fun invoke(date: LocalDate): D? = repository.getDay(date)
}

abstract class ClearPlanDayUseCase<D : PlannedDay<D>>(private val repository: DayPlanRepository<D>) {
    suspend operator fun invoke(date: LocalDate) = repository.clear(date)
}

abstract class ClearPlanDaysUseCase<D : PlannedDay<D>>(private val repository: DayPlanRepository<D>) {
    suspend operator fun invoke(dates: Set<LocalDate>): List<D> {
        val removed = repository.daysAt(dates)
        if (removed.isNotEmpty()) repository.clearAll(removed.map { it.date })
        return removed
    }
}

abstract class RestorePlanDaysUseCase<D : PlannedDay<D>>(private val repository: DayPlanRepository<D>) {
    suspend operator fun invoke(days: List<D>) = repository.saveAll(days)
}

abstract class PastePlanDaysUseCase<D : PlannedDay<D>>(
    private val repository: DayPlanRepository<D>,
    private val clock: Clock
) {
    suspend operator fun invoke(copied: Set<LocalDate>, target: LocalDate): PastedDays<D> {
        if (copied.isEmpty()) return PastedDays(emptyList(), emptyList())
        val start = copied.min()
        val now = Instant.now(clock)
        val pasted = repository.daysAt(copied).map { it.movedTo(it.date.shifted(start, target), now) }
        if (pasted.isEmpty()) return PastedDays(emptyList(), emptyList())
        val replaced = repository.daysAt(pasted.map { it.date }.toSet())
        repository.saveAll(pasted)
        return PastedDays(pasted, replaced)
    }
}

abstract class UndoPastePlanDaysUseCase<D : PlannedDay<D>>(private val repository: DayPlanRepository<D>) {
    suspend operator fun invoke(paste: PastedDays<D>) {
        repository.clearAll(paste.pasted.map { it.date })
        repository.saveAll(paste.replaced)
    }
}

private suspend fun <D : PlannedDay<D>> DayPlanRepository<D>.daysAt(dates: Set<LocalDate>): List<D> =
    if (dates.isEmpty()) emptyList() else getRange(dates.min()..dates.max()).filter { it.date in dates }
