package com.gabrieltagama.menuplanner.feature.calendar.month

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrieltagama.menuplanner.core.domain.model.AutoFillResult
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.AutoFillMonthDinnersUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.AutoFillMonthUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ObserveMonthDinnersUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ObserveMonthPlanUseCase
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
 * resumes, so the highlight stays right after midnight.
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
    val showAutoFillConfirm: Boolean = false
) {
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
}

private data class MonthPlan(val month: YearMonth, val days: Map<LocalDate, MealDay>, val dinners: Map<LocalDate, DinnerDay>)

private data class AutoFillStatus(val isRunning: Boolean = false, val showConfirm: Boolean = false)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    observeMonthPlan: ObserveMonthPlanUseCase,
    observeMonthDinners: ObserveMonthDinnersUseCase,
    private val autoFillMonth: AutoFillMonthUseCase,
    private val autoFillMonthDinners: AutoFillMonthDinnersUseCase,
    private val clock: Clock
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now(clock))
    private val selectedDate = MutableStateFlow(LocalDate.now(clock))
    private val today = MutableStateFlow(LocalDate.now(clock))
    private val view = MutableStateFlow(PlanView.LUNCH)
    private val autoFillStatus = MutableStateFlow(AutoFillStatus())

    private val monthPlan = month.flatMapLatest { visibleMonth ->
        combine(observeMonthPlan(visibleMonth), observeMonthDinners(visibleMonth)) { days, dinners ->
            MonthPlan(visibleMonth, days.associateBy { it.date }, dinners.associateBy { it.date })
        }
    }

    private val dates = combine(selectedDate, today) { selected, current -> selected to current }

    val uiState: StateFlow<CalendarUiState> =
        combine(month, dates, view, monthPlan, autoFillStatus) { visibleMonth, (selected, current), activeView, plan, status ->
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
                showAutoFillConfirm = status.showConfirm
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
        view.value = target
    }

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
