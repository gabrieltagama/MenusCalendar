package com.gabrieltagama.menuplanner.feature.calendar.month

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrieltagama.menuplanner.core.domain.model.AutoFillResult
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.AutoFillMonthDinnersUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.AutoFillMonthUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ClearDinnerDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ClearMealDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ObserveMonthDinnersUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ObserveMonthPlanUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.RestoreDinnerDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.RestoreMealDaysUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Monthly calendar: keeps the visible month, the selected day and the active view (lunches or
 * dinners), and switches the observed lunch and dinner plans with flatMapLatest whenever the
 * month changes. It also runs the random autofill of the active view for the visible month after
 * confirmation and reports its result as a one-shot event. "Today" is refreshed whenever the screen
 * resumes, so the highlight stays right after midnight. A selection mode marks planned days of the
 * active view (tap, drag over a range or the whole month, kept across months) and deletes them at
 * once; the deletion is reported as an event that carries the removed plan so it can be undone.
 */
enum class PlanView { LUNCH, DINNER }

data class CalendarUiState(
    val month: YearMonth,
    val selectedDate: LocalDate,
    val today: LocalDate,
    val view: PlanView = PlanView.LUNCH,
    val isLoading: Boolean = true,
    val days: Map<LocalDate, MealDay> = emptyMap(),
    val dinners: Map<LocalDate, DinnerDay> = emptyMap(),
    val isAutoFilling: Boolean = false,
    val showAutoFillConfirm: Boolean = false,
    val selection: DaySelection? = null
) {
    val isSelecting: Boolean get() = selection != null

    val plannedDates: Set<LocalDate> get() = when (view) {
        PlanView.LUNCH -> days.keys
        PlanView.DINNER -> dinners.keys
    }

    fun isMarked(date: LocalDate): Boolean = selection?.dates?.contains(date) == true

    val selectedDay: MealDay? get() = days[selectedDate]

    val selectedDinner: DinnerDay? get() = dinners[selectedDate]

    val isShowingCurrentMonth: Boolean get() = month == YearMonth.from(today)

    fun isPlanned(date: LocalDate): Boolean = when (view) {
        PlanView.LUNCH -> date in days
        PlanView.DINNER -> date in dinners
    }
}

sealed interface CalendarEvent {
    data class AutoFillCompleted(val result: AutoFillResult, val view: PlanView) : CalendarEvent
    data class DaysDeleted(val deletion: PlanDeletion) : CalendarEvent
}

sealed interface PlanDeletion {
    val count: Int
    val view: PlanView

    data class Lunches(val days: List<MealDay>) : PlanDeletion {
        override val count: Int get() = days.size
        override val view: PlanView get() = PlanView.LUNCH
    }

    data class Dinners(val days: List<DinnerDay>) : PlanDeletion {
        override val count: Int get() = days.size
        override val view: PlanView get() = PlanView.DINNER
    }
}

private data class MonthPlan(val month: YearMonth, val days: Map<LocalDate, MealDay>, val dinners: Map<LocalDate, DinnerDay>)

private data class AutoFillStatus(val isRunning: Boolean = false, val showConfirm: Boolean = false)

private data class Modes(val autoFill: AutoFillStatus, val selection: DaySelection?)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    observeMonthPlan: ObserveMonthPlanUseCase,
    observeMonthDinners: ObserveMonthDinnersUseCase,
    private val autoFillMonth: AutoFillMonthUseCase,
    private val autoFillMonthDinners: AutoFillMonthDinnersUseCase,
    private val clearMealDays: ClearMealDaysUseCase,
    private val clearDinnerDays: ClearDinnerDaysUseCase,
    private val restoreMealDays: RestoreMealDaysUseCase,
    private val restoreDinnerDays: RestoreDinnerDaysUseCase,
    private val clock: Clock
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now(clock))
    private val selectedDate = MutableStateFlow(LocalDate.now(clock))
    private val today = MutableStateFlow(LocalDate.now(clock))
    private val view = MutableStateFlow(PlanView.LUNCH)
    private val autoFillStatus = MutableStateFlow(AutoFillStatus())
    private val selection = MutableStateFlow<DaySelection?>(null)

    private val monthPlan = month.flatMapLatest { visibleMonth ->
        combine(observeMonthPlan(visibleMonth), observeMonthDinners(visibleMonth)) { days, dinners ->
            MonthPlan(visibleMonth, days.associateBy { it.date }, dinners.associateBy { it.date })
        }
    }

    private val dates = combine(selectedDate, today) { selected, current -> selected to current }

    private val modes = combine(autoFillStatus, selection, ::Modes)

    val uiState: StateFlow<CalendarUiState> =
        combine(month, dates, view, monthPlan, modes) { visibleMonth, (selected, current), activeView, plan, (status, marked) ->
            val isCurrentPlan = plan.month == visibleMonth
            CalendarUiState(
                month = visibleMonth,
                selectedDate = selected,
                today = current,
                view = activeView,
                isLoading = !isCurrentPlan,
                days = if (isCurrentPlan) plan.days else emptyMap(),
                dinners = if (isCurrentPlan) plan.dinners else emptyMap(),
                isAutoFilling = status.isRunning,
                showAutoFillConfirm = status.showConfirm,
                selection = marked
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CalendarUiState(month = month.value, selectedDate = selectedDate.value, today = today.value)
        )

    private val _events = Channel<CalendarEvent>(Channel.BUFFERED)
    val events: Flow<CalendarEvent> = _events.receiveAsFlow()

    fun onPreviousMonth() = showMonth(month.value.minusMonths(1))

    fun onNextMonth() = showMonth(month.value.plusMonths(1))

    fun onToday() {
        val current = refreshToday()
        month.value = YearMonth.from(current)
        selectedDate.value = current
    }

    fun onScreenResume() {
        refreshToday()
    }

    fun onDateSelect(date: LocalDate) {
        selectedDate.value = date
    }

    fun onViewChange(target: PlanView) {
        if (target != view.value) selection.update { it?.let { DaySelection() } }
        view.value = target
    }

    fun onSelectionStart() = selection.update { it ?: DaySelection() }

    fun onSelectionCancel() {
        selection.value = null
    }

    fun onSelectionToggle(date: LocalDate) = selection.update { it?.toggle(date, selectableDates()) }

    fun onSelectAllInMonth() = selection.update { it?.selectAll(selectableDates()) }

    fun onSelectionDragStart(date: LocalDate) = selection.update { (it ?: DaySelection()).startDrag(date, selectableDates()) }

    fun onSelectionDragMove(date: LocalDate) = selection.update { it?.dragTo(date, selectableDates()) }

    fun onSelectionDragEnd() = selection.update { it?.endDrag() }

    fun onDeleteSelected() {
        val marked = selection.value?.dates.orEmpty()
        if (marked.isEmpty()) return
        val targetView = view.value
        selection.value = null
        viewModelScope.launch {
            val deletion = delete(marked, targetView)
            if (deletion.count > 0) _events.send(CalendarEvent.DaysDeleted(deletion))
        }
    }

    fun onUndoDelete(deletion: PlanDeletion) {
        viewModelScope.launch {
            when (deletion) {
                is PlanDeletion.Lunches -> restoreMealDays(deletion.days)
                is PlanDeletion.Dinners -> restoreDinnerDays(deletion.days)
            }
        }
    }

    private suspend fun delete(marked: Set<LocalDate>, targetView: PlanView): PlanDeletion = when (targetView) {
        PlanView.LUNCH -> PlanDeletion.Lunches(clearMealDays(marked))
        PlanView.DINNER -> PlanDeletion.Dinners(clearDinnerDays(marked))
    }

    private fun selectableDates(): Set<LocalDate> = uiState.value.plannedDates

    fun onAutoFillRequest() = autoFillStatus.update { if (it.isRunning) it else it.copy(showConfirm = true) }

    fun onAutoFillDismiss() = autoFillStatus.update { it.copy(showConfirm = false) }

    fun onAutoFill() {
        if (autoFillStatus.value.isRunning) return
        val target = month.value
        val targetView = view.value
        autoFillStatus.value = AutoFillStatus(isRunning = true)
        viewModelScope.launch {
            try {
                _events.send(CalendarEvent.AutoFillCompleted(autoFill(target, targetView), targetView))
            } finally {
                autoFillStatus.update { it.copy(isRunning = false) }
            }
        }
    }

    private suspend fun autoFill(target: YearMonth, targetView: PlanView): AutoFillResult = when (targetView) {
        PlanView.LUNCH -> autoFillMonth(target)
        PlanView.DINNER -> autoFillMonthDinners(target)
    }

    private fun showMonth(target: YearMonth) {
        val current = refreshToday()
        month.value = target
        selectedDate.value = if (YearMonth.from(current) == target) current else target.atDay(1)
    }

    private fun refreshToday(): LocalDate = LocalDate.now(clock).also { today.value = it }
}
