package com.gabrieltagama.menuplanner.feature.calendar.month

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrieltagama.menuplanner.core.domain.model.AutoFillResult
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.model.PastedDays
import com.gabrieltagama.menuplanner.core.domain.model.plan.PlannedDinner
import com.gabrieltagama.menuplanner.core.domain.model.plan.PlannedLunch
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.AutoFillMonthDinnersUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.AutoFillMonthUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ClearDinnerDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ClearMealDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ObserveMonthDinnersUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ObserveMonthPlanUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.PasteDinnerDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.PasteMealDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.RestoreDinnerDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.RestoreMealDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.UndoPasteDinnerDaysUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.UndoPasteMealDaysUseCase
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
 * dinners), and switches the observed lunch and dinner read models (PlannedLunch, PlannedDinner)
 * with flatMapLatest whenever the month changes. It also runs the random autofill of the active view for the visible month after
 * confirmation and reports its result as a one-shot event. "Today" is refreshed whenever the screen
 * resumes, so the highlight stays right after midnight. A selection mode marks any days (tap, drag
 * over a range or the whole month, kept across months and views) and acts on the active view: it
 * deletes the planned ones, autofills the empty ones or copies them; a copy is pasted by tapping
 * the first target day. Deletions and pastes are reported as events that carry what changed so
 * they can be undone.
 */
enum class PlanView { LUNCH, DINNER }

data class CalendarUiState(
    val month: YearMonth,
    val selectedDate: LocalDate,
    val today: LocalDate,
    val view: PlanView = PlanView.LUNCH,
    val isLoading: Boolean = true,
    val days: Map<LocalDate, PlannedLunch> = emptyMap(),
    val dinners: Map<LocalDate, PlannedDinner> = emptyMap(),
    val isAutoFilling: Boolean = false,
    val showAutoFillConfirm: Boolean = false,
    val selection: DaySelection? = null,
    val clipboard: PlanClipboard? = null
) {
    val isSelecting: Boolean get() = selection != null

    val isPasting: Boolean get() = clipboard != null

    val plannedDates: Set<LocalDate> get() = when (view) {
        PlanView.LUNCH -> days.keys
        PlanView.DINNER -> dinners.keys
    }

    fun isMarked(date: LocalDate): Boolean = selection?.dates?.contains(date) == true

    val selectedDay: PlannedLunch? get() = days[selectedDate]

    val selectedDinner: PlannedDinner? get() = dinners[selectedDate]

    val isShowingCurrentMonth: Boolean get() = month == YearMonth.from(today)

    fun isPlanned(date: LocalDate): Boolean = when (view) {
        PlanView.LUNCH -> date in days
        PlanView.DINNER -> date in dinners
    }
}

sealed interface CalendarEvent {
    data class AutoFillCompleted(val result: AutoFillResult, val view: PlanView) : CalendarEvent
    data class DaysDeleted(val deletion: PlanDeletion) : CalendarEvent
    data class SelectionFilled(val result: AutoFillResult, val view: PlanView) : CalendarEvent
    data class DaysPasted(val paste: PlanPaste) : CalendarEvent
    data object NothingPlanned : CalendarEvent
}

data class PlanClipboard(val dates: Set<LocalDate>, val view: PlanView) {
    val count: Int get() = dates.size
}

sealed interface PlanPaste {
    val count: Int
    val view: PlanView

    data class Lunches(val days: PastedDays<MealDay>) : PlanPaste {
        override val count: Int get() = days.count
        override val view: PlanView get() = PlanView.LUNCH
    }

    data class Dinners(val days: PastedDays<DinnerDay>) : PlanPaste {
        override val count: Int get() = days.count
        override val view: PlanView get() = PlanView.DINNER
    }
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

private data class MonthPlan(val month: YearMonth, val days: Map<LocalDate, PlannedLunch>, val dinners: Map<LocalDate, PlannedDinner>)

private data class AutoFillStatus(val isRunning: Boolean = false, val showConfirm: Boolean = false)

private data class Modes(val autoFill: AutoFillStatus, val selection: DaySelection?, val clipboard: PlanClipboard?)

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
    private val pasteMealDays: PasteMealDaysUseCase,
    private val pasteDinnerDays: PasteDinnerDaysUseCase,
    private val undoPasteMealDays: UndoPasteMealDaysUseCase,
    private val undoPasteDinnerDays: UndoPasteDinnerDaysUseCase,
    private val clock: Clock
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now(clock))
    private val selectedDate = MutableStateFlow(LocalDate.now(clock))
    private val today = MutableStateFlow(LocalDate.now(clock))
    private val view = MutableStateFlow(PlanView.LUNCH)
    private val autoFillStatus = MutableStateFlow(AutoFillStatus())
    private val selection = MutableStateFlow<DaySelection?>(null)
    private val clipboard = MutableStateFlow<PlanClipboard?>(null)

    private val monthPlan = month.flatMapLatest { visibleMonth ->
        combine(observeMonthPlan(visibleMonth), observeMonthDinners(visibleMonth)) { days, dinners ->
            MonthPlan(visibleMonth, days.associateBy { it.date }, dinners.associateBy { it.date })
        }
    }

    private val dates = combine(selectedDate, today) { selected, current -> selected to current }

    private val modes = combine(autoFillStatus, selection, clipboard, ::Modes)

    val uiState: StateFlow<CalendarUiState> =
        combine(month, dates, view, monthPlan, modes) { visibleMonth, (selected, current), activeView, plan, (status, marked, copied) ->
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
                selection = marked,
                clipboard = copied
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

    fun onSelectionStart() {
        clipboard.value = null
        selection.update { it ?: DaySelection() }
    }

    fun onSelectionCancel() {
        selection.value = null
    }

    fun onSelectionToggle(date: LocalDate) = selection.update { it?.toggle(date) }

    fun onSelectAllInMonth() = selection.update { it?.selectAll(month.value.daysOfMonth()) }

    fun onSelectionDragStart(date: LocalDate) {
        if (clipboard.value != null) return
        selection.update { (it ?: DaySelection()).startDrag(date) }
    }

    fun onSelectionDragMove(date: LocalDate) = selection.update { it?.dragTo(date) }

    fun onSelectionDragEnd() = selection.update { it?.endDrag() }

    fun onDeleteSelected() {
        val marked = selection.value?.dates.orEmpty()
        if (marked.isEmpty()) return
        val targetView = view.value
        selection.value = null
        viewModelScope.launch {
            val deletion = delete(marked, targetView)
            _events.send(if (deletion.count > 0) CalendarEvent.DaysDeleted(deletion) else CalendarEvent.NothingPlanned)
        }
    }

    fun onFillSelected() {
        val marked = selection.value?.dates.orEmpty()
        if (marked.isEmpty() || autoFillStatus.value.isRunning) return
        val targetView = view.value
        selection.value = null
        autoFillStatus.value = AutoFillStatus(isRunning = true)
        viewModelScope.launch {
            try {
                _events.send(CalendarEvent.SelectionFilled(autoFill(marked, targetView), targetView))
            } finally {
                autoFillStatus.update { it.copy(isRunning = false) }
            }
        }
    }

    fun onCopySelected() {
        val marked = selection.value?.dates.orEmpty()
        if (marked.isEmpty()) return
        selection.value = null
        clipboard.value = PlanClipboard(marked, view.value)
    }

    fun onPasteAt(target: LocalDate) {
        val copied = clipboard.value ?: return
        clipboard.value = null
        viewModelScope.launch {
            val paste = paste(copied, target)
            _events.send(if (paste.count > 0) CalendarEvent.DaysPasted(paste) else CalendarEvent.NothingPlanned)
        }
    }

    fun onPasteCancel() {
        clipboard.value = null
    }

    fun onUndoPaste(paste: PlanPaste) {
        viewModelScope.launch {
            when (paste) {
                is PlanPaste.Lunches -> undoPasteMealDays(paste.days)
                is PlanPaste.Dinners -> undoPasteDinnerDays(paste.days)
            }
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

    private suspend fun delete(marked: Set<LocalDate>, targetView: PlanView): PlanDeletion = when (targetView) {
        PlanView.LUNCH -> PlanDeletion.Lunches(clearMealDays(marked))
        PlanView.DINNER -> PlanDeletion.Dinners(clearDinnerDays(marked))
    }

    private suspend fun paste(copied: PlanClipboard, target: LocalDate): PlanPaste = when (copied.view) {
        PlanView.LUNCH -> PlanPaste.Lunches(pasteMealDays(copied.dates, target))
        PlanView.DINNER -> PlanPaste.Dinners(pasteDinnerDays(copied.dates, target))
    }

    private suspend fun autoFill(dates: Set<LocalDate>, targetView: PlanView): AutoFillResult = when (targetView) {
        PlanView.LUNCH -> autoFillMonth(dates)
        PlanView.DINNER -> autoFillMonthDinners(dates)
    }

    private fun YearMonth.daysOfMonth(): Set<LocalDate> = (1..lengthOfMonth()).map(::atDay).toSet()

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
