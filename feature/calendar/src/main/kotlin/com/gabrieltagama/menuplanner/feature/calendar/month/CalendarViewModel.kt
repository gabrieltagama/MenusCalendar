package com.gabrieltagama.menuplanner.feature.calendar.month

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.common.fold
import com.gabrieltagama.menuplanner.core.domain.common.onFailure
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.model.plan.PlannedDinner
import com.gabrieltagama.menuplanner.core.domain.model.plan.PlannedLunch
import com.gabrieltagama.menuplanner.core.domain.query.CalendarQueries
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.PlanCommandService
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.UndoableChange
import com.gabrieltagama.menuplanner.core.ui.state.catchingStorageFailure
import com.gabrieltagama.menuplanner.core.ui.state.runStorageAction
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
 * Monthly calendar: keeps the active view (lunches or dinners) and switches the observed lunch and
 * dinner read models (CalendarQueries) with flatMapLatest whenever the month changes. The visible month, the selected
 * day and "today" (refreshed whenever the screen resumes) live in MonthNavigator, the selection and
 * copy mode in DaySelectionController and the changes on the plan of the active view (autofill,
 * delete, paste and their undo) in PlanCommandService; this class
 * only wires them to the screen, runs the autofill after confirmation and reports results as
 * one-shot events that carry what changed so deletions and pastes can be undone, or StorageFailed
 * when the device storage could not apply a change.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    calendar: CalendarQueries,
    private val commands: PlanCommandService,
    clock: Clock
) : ViewModel() {

    private val navigator = MonthNavigator(clock)
    private val view = MutableStateFlow(Meal.LUNCH)
    private val autoFillStatus = MutableStateFlow(AutoFillStatus())
    private val selection = DaySelectionController()

    private val monthPlan = navigator.month.flatMapLatest { visibleMonth ->
        combine(calendar.observeLunches(visibleMonth), calendar.observeDinners(visibleMonth)) { lunches, dinners ->
            MonthPlan(visibleMonth, lunches.associateBy { it.date }, dinners.associateBy { it.date })
        }
    }

    private val dates = combine(navigator.selectedDate, navigator.today) { selected, current -> selected to current }

    private val modes = combine(autoFillStatus, selection.mode, ::Modes)

    val uiState: StateFlow<CalendarUiState> =
        combine(navigator.month, dates, view, monthPlan, modes) { visibleMonth, (selected, current), activeView, plan, (status, mode) ->
            val isCurrentPlan = plan.month == visibleMonth
            CalendarUiState(
                month = visibleMonth,
                selectedDate = selected,
                today = current,
                view = activeView,
                isLoading = !isCurrentPlan,
                lunches = if (isCurrentPlan) plan.lunches else emptyMap(),
                dinners = if (isCurrentPlan) plan.dinners else emptyMap(),
                isAutoFilling = status.isRunning,
                showAutoFillConfirm = status.showConfirm,
                mode = mode
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CalendarUiState(month = navigator.month.value, selectedDate = navigator.selectedDate.value, today = navigator.today.value)
        )

    private val _events = Channel<CalendarEvent>(Channel.BUFFERED)
    val events: Flow<CalendarEvent> = _events.receiveAsFlow()

    fun onPreviousMonth() = navigator.previous()

    fun onNextMonth() = navigator.next()

    fun onToday() = navigator.goToToday()

    fun onScreenResume() {
        navigator.refreshToday()
    }

    fun onDateSelect(date: LocalDate) {
        navigator.select(date)
    }

    fun onViewChange(target: Meal) {
        view.value = target
    }

    fun onSelectionStart() = selection.start()

    fun onSelectionCancel() = selection.cancel()

    fun onSelectionToggle(date: LocalDate) = selection.toggle(date)

    fun onSelectAllInMonth() = selection.selectAll(navigator.daysOfMonth)

    fun onSelectionDragStart(date: LocalDate) = selection.startDrag(date)

    fun onSelectionDragMove(date: LocalDate) = selection.dragTo(date)

    fun onSelectionDragEnd() = selection.endDrag()

    fun onDeleteSelected() {
        val marked = selection.take()
        if (marked.isEmpty()) return
        val targetView = view.value
        viewModelScope.launch {
            sendResult {
                val deletion = commands.delete(marked, targetView)
                if (deletion.count > 0) CalendarEvent.DaysDeleted(deletion) else CalendarEvent.NothingPlanned
            }
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
            sendResult {
                val paste = commands.paste(copied.dates, copied.view, target)
                if (paste.count > 0) CalendarEvent.DaysPasted(paste) else CalendarEvent.NothingPlanned
            }
        }
    }

    fun onPasteCancel() = selection.cancelPaste()

    fun onUndo(change: UndoableChange) {
        viewModelScope.launch { runStorageAction { commands.undo(change) }.onFailure { _events.send(CalendarEvent.StorageFailed) } }
    }

    fun onAutoFillRequest() = autoFillStatus.update { if (it.isRunning) it else it.copy(showConfirm = true) }

    fun onAutoFillDismiss() = autoFillStatus.update { it.copy(showConfirm = false) }

    fun onAutoFill() {
        if (autoFillStatus.value.isRunning) return
        val target = navigator.month.value
        val targetView = view.value
        runAutoFill { CalendarEvent.AutoFillCompleted(commands.autoFill(target, targetView), targetView) }
    }

    private suspend fun sendResult(command: suspend () -> CalendarEvent) =
        _events.send(catchingStorageFailure { Outcome.Success(command()) }.fold(onSuccess = { it }, onFailure = { CalendarEvent.StorageFailed }))

    private fun runAutoFill(fill: suspend () -> CalendarEvent) {
        autoFillStatus.value = AutoFillStatus(isRunning = true)
        viewModelScope.launch {
            try {
                sendResult(fill)
            } finally {
                autoFillStatus.update { it.copy(isRunning = false) }
            }
        }
    }
}

private data class MonthPlan(val month: YearMonth, val lunches: Map<LocalDate, PlannedLunch>, val dinners: Map<LocalDate, PlannedDinner>)

private data class AutoFillStatus(val isRunning: Boolean = false, val showConfirm: Boolean = false)

private data class Modes(val autoFill: AutoFillStatus, val mode: CalendarMode)
