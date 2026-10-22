package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.model.AutoFillResult
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.model.PlannedDay
import com.gabrieltagama.menuplanner.core.domain.model.RepeatWindow
import com.gabrieltagama.menuplanner.core.domain.port.DayPlanRepository
import com.gabrieltagama.menuplanner.core.domain.port.DishRepository
import com.gabrieltagama.menuplanner.core.domain.port.UserPreferencesRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.first

/**
 * Template of the lunch and dinner autofill: fills every empty day of [meal] in a month (from today
 * on in the current month) or in a given set of dates; planned days are never modified. Lunches and
 * dinners of the neighbouring days within the user's RepeatWindow are read once into a PlanHistory
 * (PlanHistoryReader), so the rules also hold across month boundaries. Each meal only says how one day is picked
 * (pickerFor); every filled day is saved at the end in a single transaction.
 */
abstract class AutoFillPlanUseCase<D : PlannedDay<D>> internal constructor(
    private val meal: Meal,
    private val repository: DayPlanRepository<D>,
    private val dishRepository: DishRepository,
    private val historyReader: PlanHistoryReader,
    private val preferencesRepository: UserPreferencesRepository,
    private val clock: Clock
) {
    internal abstract fun pickerFor(dishes: List<Dish>, window: RepeatWindow): DayPicker<D>

    suspend operator fun invoke(month: YearMonth): AutoFillResult = invoke(month.daysToFill(LocalDate.now(clock)))

    suspend operator fun invoke(dates: Collection<LocalDate>): AutoFillResult {
        if (dates.isEmpty()) return AutoFillResult.EMPTY
        val window = preferencesRepository.repeatWindow.first()
        val picker = pickerFor(dishRepository.getDishes(), window)
        val range = window.historyRange(dates.min()..dates.max())
        val history = historyReader.read(range)
        val now = clock.instant()
        val fill = fillEmptyDays(dates.sorted().filterNot { history.isPlanned(it, meal) }) { date -> picker.pick(date, history, now) }
        repository.saveAll(fill.days)
        return fill.result
    }
}

/**
 * Picks the plan of one day and records it in the history, or returns null when nothing fits.
 */
internal fun interface DayPicker<D> {
    fun pick(date: LocalDate, history: PlanHistory, at: Instant): DayPick<D>?
}
