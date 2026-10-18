package com.gabrieltagama.menuplanner.feature.calendar.month

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrieltagama.menuplanner.core.domain.model.plan.PlannedDinner
import com.gabrieltagama.menuplanner.core.domain.model.plan.PlannedLunch
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
 * dinners), and switches the observed lunch and dinner read models with flatMapLatest whenever the
 * month changes. "Today" is refreshed whenever the screen resumes, so the highlight stays right
 * after midnight. The selection and copy mode lives in DaySelectionController and the changes on
 * the plan of the active view (autofill, delete, paste and their undo) in PlanCommands; this class
 * only wires them to the screen, runs the autofill after confirmation and reports results as
 * one-shot events that carry what changed so deletions and pastes can be undone.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    observeMonthPlan: ObserveMonthPlanUseCase,
    observeMonthDinners: ObserveMonthDinnersUseCase,
    private val commands: PlanCommands,
    private val clock: Clock
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now(clock))
    private val selectedDate = MutableStateFlow(LocalDate.now(clock))
    private val today = MutableStateFlow(LocalDate.now(clock))
    private val view = MutableStateFlow(PlanView.LUNCH)
    private val autoFillStatus = MutableStateFlow(AutoFillStatus())
    private val selection = DaySelectionController()

    private val monthPlan = month.flatMapLatest { visibleMonth ->
        combine(observeMonthPlan(visibleMonth), observeMonthDinners(visibleMonth)) { days, dinners ->
            MonthPlan(visibleMonth, days.associateBy { it.date }, dinners.associateBy { it.date })
        }
    }

    private val dates = combine(selectedDate, today) { selected, current -> selected to current }

    private val modes = combine(autoFillStatus, selection.mode, ::Modes)

    val uiState: StateFlow<CalendarUiState> =
        combine(month, dates, view, monthPlan, modes) { visibleMonth, (selected, current), activeView, plan, (status, mode) ->
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
                mode = mode
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

    fun onSelectionStart() = selection.start()

    fun onSelectionCancel() = selection.cancel()

    fun onSelectionToggle(date: LocalDate) = selection.toggle(date)

    fun onSelectAllInMonth() = selection.selectAll(month.value.daysOfMonth())

    fun onSelectionDragStart(date: LocalDate) = selection.startDrag(date)

    fun onSelectionDragMove(date: LocalDate) = selection.dragTo(date)

    fun onSelectionDragEnd() = selection.endDrag()

    fun onDeleteSelected() {
        val marked = selection.take()
        if (marked.isEmpty()) return
        val targetView = view.value
        viewModelScope.launch {
            val deletion = commands.delete(marked, targetView)
            _events.send(if (deletion.count > 0) CalendarEvent.DaysDeleted(deletion) else CalendarEvent.NothingPlanned)
        }
    }

    fun onFillSelected() {
        if (autoFillStatus.value.isRunning) return
        val marked = selection.take()
        if (marked.isEmpty()) return
        val targetView = view.value
        runAutoFill { CalendarEvent.SelectionFilled(commands.autoFill(marked, targetView), targetView) }
    }

    fun onCopySelected() {
        selection.copy(view.value)
    }

    fun onPasteAt(target: LocalDate) {
        val copied = selection.takeClipboard() ?: return
        viewModelScope.launch {
            val paste = commands.paste(copied, target)
            _events.send(if (paste.count > 0) CalendarEvent.DaysPasted(paste) else CalendarEvent.NothingPlanned)
        }
    }

    fun onPasteCancel() = selection.cancelPaste()

    fun onUndoPaste(paste: PlanPaste) {
        viewModelScope.launch { commands.undo(paste) }
    }

    fun onUndoDelete(deletion: PlanDeletion) {
        viewModelScope.launch { commands.undo(deletion) }
    }

    fun onAutoFillRequest() = autoFillStatus.update { if (it.isRunning) it else it.copy(showConfirm = true) }

    fun onAutoFillDismiss() = autoFillStatus.update { it.copy(showConfirm = false) }

    fun onAutoFill() {
        if (autoFillStatus.value.isRunning) return
        val target = month.value
        val targetView = view.value
        runAutoFill { CalendarEvent.AutoFillCompleted(commands.autoFill(target, targetView), targetView) }
    }

    private fun runAutoFill(fill: suspend () -> CalendarEvent) {
        autoFillStatus.value = AutoFillStatus(isRunning = true)
        viewModelScope.launch {
            try {
                _events.send(fill())
            } finally {
                autoFillStatus.update { it.copy(isRunning = false) }
            }
        }
    }

    private fun YearMonth.daysOfMonth(): Set<LocalDate> = (1..lengthOfMonth()).map(::atDay).toSet()

    private fun showMonth(target: YearMonth) {
        val current = refreshToday()
        month.value = target
        selectedDate.value = if (YearMonth.from(current) == target) current else target.atDay(1)
    }

    private fun refreshToday(): LocalDate = LocalDate.now(clock).also { today.value = it }
}

private data class MonthPlan(val month: YearMonth, val days: Map<LocalDate, PlannedLunch>, val dinners: Map<LocalDate, PlannedDinner>)

private data class AutoFillStatus(val isRunning: Boolean = false, val showConfirm: Boolean = false)

private data class Modes(val autoFill: AutoFillStatus, val mode: CalendarMode)
