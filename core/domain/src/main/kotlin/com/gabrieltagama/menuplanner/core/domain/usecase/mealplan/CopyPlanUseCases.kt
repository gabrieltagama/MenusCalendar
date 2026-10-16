package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.model.PastedDays
import com.gabrieltagama.menuplanner.core.domain.model.shifted
import com.gabrieltagama.menuplanner.core.domain.repository.DinnerPlanRepository
import com.gabrieltagama.menuplanner.core.domain.repository.MealPlanRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/**
 * Copy and paste of lunches and dinners. The planned days among the copied dates are written
 * from the target date on, keeping the gaps between them (the first copied date lands on the
 * target), and overwrite what was planned there. Undoing removes the pasted days and puts back
 * the replaced ones.
 */
class PasteMealDaysUseCase @Inject constructor(
    private val repository: MealPlanRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(copied: Set<LocalDate>, target: LocalDate): PastedDays<MealDay> {
        if (copied.isEmpty()) return PastedDays(emptyList(), emptyList())
        val start = copied.min()
        val now = Instant.now(clock)
        val pasted = repository.getRange(start..copied.max())
            .filter { it.date in copied }
            .map { it.copy(date = it.date.shifted(start, target), updatedAt = now) }
        if (pasted.isEmpty()) return PastedDays(emptyList(), emptyList())
        val targetDates = pasted.map(MealDay::date).toSet()
        val replaced = repository.getRange(targetDates.min()..targetDates.max()).filter { it.date in targetDates }
        repository.saveAll(pasted)
        return PastedDays(pasted, replaced)
    }
}

class UndoPasteMealDaysUseCase @Inject constructor(private val repository: MealPlanRepository) {
    suspend operator fun invoke(paste: PastedDays<MealDay>) {
        repository.clearAll(paste.pasted.map(MealDay::date))
        repository.saveAll(paste.replaced)
    }
}

class PasteDinnerDaysUseCase @Inject constructor(
    private val repository: DinnerPlanRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(copied: Set<LocalDate>, target: LocalDate): PastedDays<DinnerDay> {
        if (copied.isEmpty()) return PastedDays(emptyList(), emptyList())
        val start = copied.min()
        val now = Instant.now(clock)
        val pasted = repository.getRange(start..copied.max())
            .filter { it.date in copied }
            .map { it.copy(date = it.date.shifted(start, target), updatedAt = now) }
        if (pasted.isEmpty()) return PastedDays(emptyList(), emptyList())
        val targetDates = pasted.map(DinnerDay::date).toSet()
        val replaced = repository.getRange(targetDates.min()..targetDates.max()).filter { it.date in targetDates }
        repository.saveAll(pasted)
        return PastedDays(pasted, replaced)
    }
}

class UndoPasteDinnerDaysUseCase @Inject constructor(private val repository: DinnerPlanRepository) {
    suspend operator fun invoke(paste: PastedDays<DinnerDay>) {
        repository.clearAll(paste.pasted.map(DinnerDay::date))
        repository.saveAll(paste.replaced)
    }
}
