package com.gabrieltagama.menuplanner.feature.calendar.month

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrieltagama.menuplanner.core.domain.model.AutoFillResult
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.AutoFillMonthUseCase
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Monthly calendar: keeps the visible month and the selected day, and switches the observed
 * month plan with flatMapLatest whenever the month changes. It also runs the random autofill of
 * the visible month after confirmation and reports its result as a one-shot event.
 */
data class CalendarUiState(
    val month: YearMonth,
    val selectedDate: LocalDate,
    val today: LocalDate,
    val isLoading: Boolean = true,
    val days: Map<LocalDate, MealDay> = emptyMap(),
    val isAutoFilling: Boolean = false,
    val showAutoFillConfirm: Boolean = false
) {
    val selectedDay: MealDay? get() = days[selectedDate]
}

sealed interface CalendarEvent {
    data class AutoFillCompleted(val result: AutoFillResult) : CalendarEvent
}

private data class MonthPlan(val month: YearMonth, val days: Map<LocalDate, MealDay>)

private data class AutoFillStatus(val isRunning: Boolean = false, val showConfirm: Boolean = false)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    observeMonthPlan: ObserveMonthPlanUseCase,
    private val autoFillMonth: AutoFillMonthUseCase,
    private val clock: Clock
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now(clock))
    private val selectedDate = MutableStateFlow(LocalDate.now(clock))
    private val autoFillStatus = MutableStateFlow(AutoFillStatus())

    private val monthPlan = month.flatMapLatest { visibleMonth ->
        observeMonthPlan(visibleMonth).map { days -> MonthPlan(visibleMonth, days.associateBy { it.date }) }
    }

    val uiState: StateFlow<CalendarUiState> =
        combine(month, selectedDate, monthPlan, autoFillStatus) { visibleMonth, selected, plan, status ->
            val isCurrentPlan = plan.month == visibleMonth
            CalendarUiState(
                month = visibleMonth,
                selectedDate = selected,
                today = LocalDate.now(clock),
                isLoading = !isCurrentPlan,
                days = if (isCurrentPlan) plan.days else emptyMap(),
                isAutoFilling = status.isRunning,
                showAutoFillConfirm = status.showConfirm
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CalendarUiState(month = month.value, selectedDate = selectedDate.value, today = LocalDate.now(clock))
        )

    private val _events = Channel<CalendarEvent>(Channel.BUFFERED)
    val events: Flow<CalendarEvent> = _events.receiveAsFlow()

    fun onPreviousMonth() = showMonth(month.value.minusMonths(1))

    fun onNextMonth() = showMonth(month.value.plusMonths(1))

    fun onToday() {
        val today = LocalDate.now(clock)
        month.value = YearMonth.from(today)
        selectedDate.value = today
    }

    fun onDateSelect(date: LocalDate) {
        selectedDate.value = date
    }

    fun onAutoFillRequest() = autoFillStatus.update { if (it.isRunning) it else it.copy(showConfirm = true) }

    fun onAutoFillDismiss() = autoFillStatus.update { it.copy(showConfirm = false) }

    fun onAutoFill() {
        if (autoFillStatus.value.isRunning) return
        val target = month.value
        autoFillStatus.value = AutoFillStatus(isRunning = true)
        viewModelScope.launch {
            try {
                _events.send(CalendarEvent.AutoFillCompleted(autoFillMonth(target)))
            } finally {
                autoFillStatus.update { it.copy(isRunning = false) }
            }
        }
    }

    private fun showMonth(target: YearMonth) {
        val today = LocalDate.now(clock)
        month.value = target
        selectedDate.value = if (YearMonth.from(today) == target) today else target.atDay(1)
    }
}
