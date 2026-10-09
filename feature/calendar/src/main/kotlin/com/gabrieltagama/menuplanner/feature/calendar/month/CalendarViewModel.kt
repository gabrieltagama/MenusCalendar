package com.gabrieltagama.menuplanner.feature.calendar.month

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ObserveMonthPlanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Monthly calendar: keeps the visible month and the selected day, and switches the observed
 * month plan with flatMapLatest whenever the month changes.
 */
data class CalendarUiState(
    val month: YearMonth,
    val selectedDate: LocalDate,
    val today: LocalDate,
    val isLoading: Boolean = true,
    val days: Map<LocalDate, MealDay> = emptyMap()
) {
    val selectedDay: MealDay? get() = days[selectedDate]
}

private data class MonthPlan(val month: YearMonth, val days: Map<LocalDate, MealDay>)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    observeMonthPlan: ObserveMonthPlanUseCase,
    private val clock: Clock
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now(clock))
    private val selectedDate = MutableStateFlow(LocalDate.now(clock))

    private val monthPlan = month.flatMapLatest { visibleMonth ->
        observeMonthPlan(visibleMonth).map { days -> MonthPlan(visibleMonth, days.associateBy { it.date }) }
    }

    val uiState: StateFlow<CalendarUiState> =
        combine(month, selectedDate, monthPlan) { visibleMonth, selected, plan ->
            val isCurrentPlan = plan.month == visibleMonth
            CalendarUiState(
                month = visibleMonth,
                selectedDate = selected,
                today = LocalDate.now(clock),
                isLoading = !isCurrentPlan,
                days = if (isCurrentPlan) plan.days else emptyMap()
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CalendarUiState(month = month.value, selectedDate = selectedDate.value, today = LocalDate.now(clock))
        )

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

    private fun showMonth(target: YearMonth) {
        val today = LocalDate.now(clock)
        month.value = target
        selectedDate.value = if (YearMonth.from(today) == target) today else target.atDay(1)
    }
}
