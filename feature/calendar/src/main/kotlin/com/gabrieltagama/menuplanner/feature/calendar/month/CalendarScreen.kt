package com.gabrieltagama.menuplanner.feature.calendar.month

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.model.MenuSlot
import com.gabrieltagama.menuplanner.core.domain.model.plan.DishSummary
import com.gabrieltagama.menuplanner.core.domain.model.plan.PlannedLunch
import com.gabrieltagama.menuplanner.core.domain.model.plan.toPlannedLunch
import com.gabrieltagama.menuplanner.core.ui.component.ConfirmDialog
import com.gabrieltagama.menuplanner.core.ui.component.DishTypeAvatar
import com.gabrieltagama.menuplanner.core.ui.component.HeavinessChip
import com.gabrieltagama.menuplanner.core.ui.component.HeavinessDot
import com.gabrieltagama.menuplanner.core.ui.preview.PreviewDishes
import com.gabrieltagama.menuplanner.core.ui.theme.MenuPlannerTheme
import com.gabrieltagama.menuplanner.feature.calendar.R
import com.gabrieltagama.menuplanner.feature.calendar.common.inlineText
import com.gabrieltagama.menuplanner.feature.calendar.common.longTitleText
import com.gabrieltagama.menuplanner.feature.calendar.common.titleText
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/**
 * Monthly calendar: stateful route bound to [CalendarViewModel] and a stateless screen with a
 * lunches/dinners switch on top, a Monday-first month grid of the active view (swipe left/right
 * to change month, one heaviness dot per planned dish) and an animated summary card of the
 * selected day, which opens the lunch or the dinner editor. The random autofill of the active
 * view for the visible month is a FAB confirmed with a dialog and summarised in a snackbar;
 * "Hoy" only appears when another month is shown. Below the day summary a button opens the
 * shopping list of the visible month. Bottom insets are left to the app shell, which draws the
 * NavigationBar below this screen. The selection button (or a long press on a day) opens a
 * selection mode with a contextual top bar: tap toggles any day, dragging marks a continuous range
 * and month swipe is paused. A bottom bar acts on the active view: fill the empty marked days,
 * copy them (then tap the first target day to paste) or delete the planned ones; deletions and
 * pastes can be undone from the snackbar.
 */
data class AutoFillActions(
    val onRequest: () -> Unit = {},
    val onConfirm: () -> Unit = {},
    val onDismiss: () -> Unit = {}
)

data class SelectionActions(
    val onStart: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val onToggle: (LocalDate) -> Unit = {},
    val onSelectAll: () -> Unit = {},
    val onDragStart: (LocalDate) -> Unit = {},
    val onDragMove: (LocalDate) -> Unit = {},
    val onDragEnd: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onFill: () -> Unit = {},
    val onCopy: () -> Unit = {},
    val onPasteAt: (LocalDate) -> Unit = {},
    val onPasteCancel: () -> Unit = {}
)

private const val MaxDotsPerDay = 3
private val FabClearance = 96.dp
private val CellHeight = 56.dp
private val GridGap = 4.dp

@Composable
fun CalendarScreenRoute(
    onOpenDay: (LocalDate) -> Unit,
    onOpenDinner: (LocalDate) -> Unit,
    onOpenShoppingList: (YearMonth) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val formatter = remember(resources) { AutoFillMessageFormatter(ResourcesAutoFillTexts(resources)) }
    val undoLabel = stringResource(R.string.calendar_undo)

    LifecycleResumeEffect(viewModel) {
        viewModel.onScreenResume()
        onPauseOrDispose { }
    }

    LaunchedEffect(viewModel, formatter) {
        viewModel.events.collect { event ->
            when (event) {
                is CalendarEvent.AutoFillCompleted -> snackbarHostState.showSnackbar(formatter.format(event.result, event.view))
                is CalendarEvent.DaysDeleted -> {
                    val message = resources.getQuantityString(event.deletion.view.deletedTextRes(), event.deletion.count, event.deletion.count)
                    val result = snackbarHostState.showSnackbar(message, actionLabel = undoLabel, duration = SnackbarDuration.Long)
                    if (result == SnackbarResult.ActionPerformed) viewModel.onUndoDelete(event.deletion)
                }
                is CalendarEvent.DaysPasted -> {
                    val message = resources.getQuantityString(event.paste.view.pastedTextRes(), event.paste.count, event.paste.count)
                    val result = snackbarHostState.showSnackbar(message, actionLabel = undoLabel, duration = SnackbarDuration.Long)
                    if (result == SnackbarResult.ActionPerformed) viewModel.onUndoPaste(event.paste)
                }
                is CalendarEvent.SelectionFilled -> snackbarHostState.showSnackbar(
                    if (event.result.filledDays == 0 && event.result.unfilledDays == 0) resources.getString(R.string.calendar_selection_nothing_to_fill)
                    else formatter.format(event.result, event.view)
                )
                CalendarEvent.NothingPlanned -> snackbarHostState.showSnackbar(resources.getString(R.string.calendar_selection_nothing_planned))
            }
        }
    }

    CalendarScreen(
        state = state,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onToday = viewModel::onToday,
        onDateSelect = viewModel::onDateSelect,
        onOpenDay = onOpenDay,
        onOpenDinner = onOpenDinner,
        onViewChange = viewModel::onViewChange,
        onOpenShoppingList = { onOpenShoppingList(state.month) },
        autoFillActions = AutoFillActions(
            onRequest = viewModel::onAutoFillRequest,
            onConfirm = viewModel::onAutoFill,
            onDismiss = viewModel::onAutoFillDismiss
        ),
        selectionActions = SelectionActions(
            onStart = viewModel::onSelectionStart,
            onCancel = viewModel::onSelectionCancel,
            onToggle = viewModel::onSelectionToggle,
            onSelectAll = viewModel::onSelectAllInMonth,
            onDragStart = viewModel::onSelectionDragStart,
            onDragMove = viewModel::onSelectionDragMove,
            onDragEnd = viewModel::onSelectionDragEnd,
            onDelete = viewModel::onDeleteSelected,
            onFill = viewModel::onFillSelected,
            onCopy = viewModel::onCopySelected,
            onPasteAt = viewModel::onPasteAt,
            onPasteCancel = viewModel::onPasteCancel
        ),
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onDateSelect: (LocalDate) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onOpenDinner: (LocalDate) -> Unit = {},
    onViewChange: (PlanView) -> Unit = {},
    onOpenShoppingList: () -> Unit = {},
    autoFillActions: AutoFillActions = AutoFillActions(),
    selectionActions: SelectionActions = SelectionActions(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val scrollState = rememberScrollState()
    BackHandler(enabled = state.isSelecting, onBack = selectionActions.onCancel)
    BackHandler(enabled = state.isPasting, onBack = selectionActions.onPasteCancel)
    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
        topBar = {
            val selection = state.selection
            val clipboard = state.clipboard
            when {
                clipboard != null -> PasteTopBar(count = clipboard.count, onPreviousMonth = onPreviousMonth, onNextMonth = onNextMonth, onCancel = selectionActions.onPasteCancel)
                selection != null -> SelectionTopBar(count = selection.count, onPreviousMonth = onPreviousMonth, onNextMonth = onNextMonth, actions = selectionActions)
                else -> MonthTopBar(state = state, onToday = onToday, onPreviousMonth = onPreviousMonth, onNextMonth = onNextMonth, onStartSelection = selectionActions.onStart)
            }
        },
        bottomBar = {
            val selection = state.selection
            if (selection != null) SelectionActionBar(hasMarkedDays = selection.count > 0 && !state.isAutoFilling, actions = selectionActions)
        },
        floatingActionButton = {
            if (!state.isSelecting && !state.isPasting && !state.isLoading && !state.isAutoFilling)
                AutoFillFab(scrollState = scrollState, onClick = autoFillActions.onRequest)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.showAutoFillConfirm)
            ConfirmDialog(
                title = stringResource(R.string.calendar_autofill_title),
                text = stringResource(state.view.autoFillTextRes(), state.autoFillPeriodText()),
                onConfirm = autoFillActions.onConfirm,
                onDismiss = autoFillActions.onDismiss,
                confirmLabel = stringResource(R.string.calendar_autofill_confirm)
            )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(start = 12.dp, end = 12.dp, bottom = FabClearance),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                if (state.isLoading || state.isAutoFilling) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            MealViewSwitch(view = state.view, onViewChange = onViewChange)
            MonthCard(state = state, onDateSelect = onDateSelect, onPreviousMonth = onPreviousMonth, onNextMonth = onNextMonth, selectionActions = selectionActions)
            DaySummaryCard(
                date = state.selectedDate,
                lines = state.summaryLines(),
                view = state.view,
                onOpen = { if (state.view == PlanView.LUNCH) onOpenDay(state.selectedDate) else onOpenDinner(state.selectedDate) }
            )
            OutlinedButton(onClick = onOpenShoppingList, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.ShoppingCart, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.calendar_shopping_list))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthTopBar(
    state: CalendarUiState,
    onToday: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onStartSelection: () -> Unit
) = TopAppBar(
    title = { Text(state.month.titleText()) },
    actions = {
        if (!state.isShowingCurrentMonth) TextButton(onClick = onToday) { Text(stringResource(R.string.calendar_today)) }
        IconButton(onClick = onStartSelection, enabled = !state.isLoading && !state.isAutoFilling) {
            Icon(Icons.Filled.Checklist, contentDescription = stringResource(R.string.calendar_selection_start))
        }
        MonthArrows(onPreviousMonth = onPreviousMonth, onNextMonth = onNextMonth)
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(count: Int, onPreviousMonth: () -> Unit, onNextMonth: () -> Unit, actions: SelectionActions) = TopAppBar(
    navigationIcon = {
        IconButton(onClick = actions.onCancel) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.calendar_selection_cancel))
        }
    },
    title = {
        Text(if (count == 0) stringResource(R.string.calendar_selection_hint) else pluralStringResource(R.plurals.calendar_selection_count, count, count))
    },
    actions = {
        IconButton(onClick = actions.onSelectAll) {
            Icon(Icons.Filled.SelectAll, contentDescription = stringResource(R.string.calendar_selection_all))
        }
        MonthArrows(onPreviousMonth = onPreviousMonth, onNextMonth = onNextMonth)
    }
)

@Composable
private fun MonthArrows(onPreviousMonth: () -> Unit, onNextMonth: () -> Unit) {
    IconButton(onClick = onPreviousMonth) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.calendar_previous_month))
    }
    IconButton(onClick = onNextMonth) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.calendar_next_month))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasteTopBar(count: Int, onPreviousMonth: () -> Unit, onNextMonth: () -> Unit, onCancel: () -> Unit) = TopAppBar(
    navigationIcon = {
        IconButton(onClick = onCancel) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.calendar_paste_cancel))
        }
    },
    title = { Text(pluralStringResource(R.plurals.calendar_paste_hint, count, count)) },
    actions = { MonthArrows(onPreviousMonth = onPreviousMonth, onNextMonth = onNextMonth) }
)

@Composable
private fun SelectionActionBar(hasMarkedDays: Boolean, actions: SelectionActions) = Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    modifier = Modifier.fillMaxWidth()
) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SelectionActionButton(text = stringResource(R.string.calendar_selection_fill), icon = Icons.Filled.AutoAwesome, enabled = hasMarkedDays, onClick = actions.onFill)
        SelectionActionButton(text = stringResource(R.string.calendar_selection_copy), icon = Icons.Filled.ContentCopy, enabled = hasMarkedDays, onClick = actions.onCopy)
        SelectionActionButton(
            text = stringResource(R.string.calendar_selection_delete),
            icon = Icons.Filled.Delete,
            enabled = hasMarkedDays,
            onClick = actions.onDelete,
            isDestructive = true
        )
    }
}

@Composable
private fun RowScope.SelectionActionButton(text: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit, isDestructive: Boolean = false) = FilledTonalButton(
    onClick = onClick,
    enabled = enabled,
    modifier = Modifier.weight(1f),
    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
    colors = if (isDestructive)
        ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
    else ButtonDefaults.filledTonalButtonColors()
) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(modifier = Modifier.width(6.dp))
    Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun AutoFillFab(scrollState: ScrollState, onClick: () -> Unit) = ExtendedFloatingActionButton(
    text = { Text(stringResource(R.string.calendar_autofill_short)) },
    icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = stringResource(R.string.calendar_autofill)) },
    onClick = onClick,
    expanded = scrollState.value == 0
)

@Composable
private fun MonthCard(
    state: CalendarUiState,
    onDateSelect: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    selectionActions: SelectionActions
) = Surface(
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    modifier = Modifier.fillMaxWidth().monthSwipe(enabled = !state.isSelecting, onPreviousMonth = onPreviousMonth, onNextMonth = onNextMonth)
) {
    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        WeekdayHeader()
        AnimatedContent(targetState = state.month, transitionSpec = { monthTransition() }, label = "month") { month ->
            MonthGrid(month = month, state = state, onDateSelect = onDateSelect, selectionActions = selectionActions)
        }
    }
}

@Composable
private fun Modifier.monthSwipe(enabled: Boolean, onPreviousMonth: () -> Unit, onNextMonth: () -> Unit): Modifier {
    val currentOnPrevious by rememberUpdatedState(onPreviousMonth)
    val currentOnNext by rememberUpdatedState(onNextMonth)
    if (!enabled) return this
    return pointerInput(Unit) {
        val threshold = 64.dp.toPx()
        var totalDrag = 0f
        detectHorizontalDragGestures(
            onDragStart = { totalDrag = 0f },
            onDragEnd = {
                when {
                    totalDrag > threshold -> currentOnPrevious()
                    totalDrag < -threshold -> currentOnNext()
                }
            }
        ) { _, dragAmount -> totalDrag += dragAmount }
    }
}

private fun AnimatedContentTransitionScope<YearMonth>.monthTransition(): ContentTransform {
    val direction = if (targetState > initialState) 1 else -1
    return (slideInHorizontally { width -> direction * width / 3 } + fadeIn())
        .togetherWith(slideOutHorizontally { width -> -direction * width / 3 } + fadeOut())
}

@Composable
private fun WeekdayHeader() = Row(modifier = Modifier.fillMaxWidth()) {
    stringArrayResource(R.array.calendar_weekday_initials).forEach { initial ->
        Text(
            text = initial,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MonthGrid(month: YearMonth, state: CalendarUiState, onDateSelect: (LocalDate) -> Unit, selectionActions: SelectionActions) = Column(
    modifier = Modifier.daySelectionGestures(month = month, isSelecting = state.isSelecting, isEnabled = !state.isPasting, actions = selectionActions),
    verticalArrangement = Arrangement.spacedBy(GridGap)
) {
    month.weeks().forEach { week ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GridGap)) {
            week.forEach { date ->
                Box(modifier = Modifier.weight(1f)) {
                    if (date != null)
                        DayCell(
                            date = date,
                            dishes = state.plannedDishes(date),
                            isToday = date == state.today,
                            isSelected = !state.isSelecting && !state.isPasting && date == state.selectedDate,
                            isMarked = state.isMarked(date),
                            onClick = {
                                when {
                                    state.isPasting -> selectionActions.onPasteAt(date)
                                    state.isSelecting -> selectionActions.onToggle(date)
                                    else -> onDateSelect(date)
                                }
                            }
                        )
                }
            }
        }
    }
}

@Composable
private fun Modifier.daySelectionGestures(month: YearMonth, isSelecting: Boolean, isEnabled: Boolean, actions: SelectionActions): Modifier {
    val currentIsSelecting by rememberUpdatedState(isSelecting)
    val currentIsEnabled by rememberUpdatedState(isEnabled)
    val currentActions by rememberUpdatedState(actions)
    val haptics = LocalHapticFeedback.current
    return pointerInput(month) {
        val gap = GridGap.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (!currentIsEnabled) return@awaitEachGesture
            val start =
                if (currentIsSelecting) awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                else awaitLongPressOrCancellation(down.id)
            if (start == null) return@awaitEachGesture
            if (!currentIsSelecting) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            start.consume()
            currentActions.onDragStart(month.dateAt(down.position, size, gap))
            var lastDate = month.dateAt(start.position, size, gap)
            currentActions.onDragMove(lastDate)
            while (true) {
                val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == start.id } ?: break
                change.consume()
                if (!change.pressed) break
                val date = month.dateAt(change.position, size, gap)
                if (date == lastDate) continue
                lastDate = date
                currentActions.onDragMove(date)
            }
            currentActions.onDragEnd()
        }
    }
}

private fun YearMonth.dateAt(position: Offset, size: IntSize, gap: Float): LocalDate {
    val weeks = weeks()
    val column = (position.x / (size.width + gap) * 7).toInt().coerceIn(0, 6)
    val row = (position.y / (size.height + gap) * weeks.size).toInt().coerceIn(0, weeks.lastIndex)
    return weeks[row][column] ?: if (row == 0) atDay(1) else atEndOfMonth()
}

@Composable
private fun DayCell(date: LocalDate, dishes: List<DishSummary>, isToday: Boolean, isSelected: Boolean, isMarked: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val background = when {
        isMarked -> colors.tertiary
        isSelected -> colors.primary
        isToday -> colors.primaryContainer
        dishes.isNotEmpty() -> colors.surfaceContainerHighest
        else -> Color.Transparent
    }
    val textColor = when {
        isMarked -> colors.onTertiary
        isSelected -> colors.onPrimary
        isToday -> colors.onPrimaryContainer
        else -> colors.onSurface
    }
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(CellHeight)
            .clip(shape)
            .background(background)
            .border(width = 1.dp, color = if (isToday && !isSelected) colors.primary else Color.Transparent, shape = shape)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
        if (isMarked)
            Icon(Icons.Filled.CheckCircle, contentDescription = stringResource(R.string.calendar_selection_marked), tint = colors.onTertiary, modifier = Modifier.size(14.dp))
        else
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.height(6.dp)) {
                dishes.take(MaxDotsPerDay).forEach { HeavinessDot(heaviness = it.heaviness, size = 6.dp) }
            }
    }
}

@Composable
private fun DaySummaryCard(date: LocalDate, lines: List<Pair<String, DishSummary>>, view: PlanView, onOpen: () -> Unit) = Surface(
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surfaceContainer,
    modifier = Modifier.fillMaxWidth()
) {
    AnimatedContent(targetState = Triple(date, view, lines), transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "day") { (shownDate, shownView, shownLines) ->
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = shownDate.longTitleText().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleLarge
            )
            if (shownLines.isEmpty()) EmptyDaySummary(view = shownView, onOpenDay = onOpen)
            else PlannedDaySummary(lines = shownLines, onOpenDay = onOpen)
        }
    }
}

@Composable
private fun EmptyDaySummary(view: PlanView, onOpenDay: () -> Unit) = Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Filled.EventBusy, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = stringResource(if (view == PlanView.LUNCH) R.string.calendar_no_menu else R.string.calendar_no_dinner),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    FilledTonalButton(onClick = onOpenDay, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.size(8.dp))
        Text(stringResource(R.string.calendar_plan))
    }
}

@Composable
private fun PlannedDaySummary(lines: List<Pair<String, DishSummary>>, onOpenDay: () -> Unit) = Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    lines.forEach { (label, dish) -> SummaryLine(label = label, dish = dish) }
    OutlinedButton(onClick = onOpenDay, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.size(8.dp))
        Text(stringResource(R.string.calendar_edit))
    }
}

@Composable
private fun SummaryLine(label: String, dish: DishSummary) = Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
) {
    DishTypeAvatar(type = dish.type, size = 36.dp)
    Column(modifier = Modifier.weight(1f)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = dish.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
    HeavinessChip(heaviness = dish.heaviness)
}

@Composable
private fun CalendarUiState.summaryLines(): List<Pair<String, DishSummary>> = when (view) {
    PlanView.LUNCH -> selectedDay?.summaryLines().orEmpty()
    PlanView.DINNER -> listOfNotNull(selectedDinner?.let { stringResource(R.string.calendar_slot_dinner) to it.dish })
}

private fun CalendarUiState.plannedDishes(date: LocalDate): List<DishSummary> = when (view) {
    PlanView.LUNCH -> days[date]?.dishes.orEmpty()
    PlanView.DINNER -> listOfNotNull(dinners[date]?.dish)
}

@Composable
private fun CalendarUiState.autoFillPeriodText(): String =
    if (isShowingCurrentMonth) stringResource(R.string.calendar_autofill_from_today, month.inlineText()) else month.inlineText()

private fun PlanView.autoFillTextRes(): Int = when (this) {
    PlanView.LUNCH -> R.string.calendar_autofill_text
    PlanView.DINNER -> R.string.calendar_autofill_dinner_text
}

private fun PlanView.deletedTextRes(): Int = when (this) {
    PlanView.LUNCH -> R.plurals.calendar_deleted_lunches
    PlanView.DINNER -> R.plurals.calendar_deleted_dinners
}

private fun PlanView.pastedTextRes(): Int = when (this) {
    PlanView.LUNCH -> R.plurals.calendar_pasted_lunches
    PlanView.DINNER -> R.plurals.calendar_pasted_dinners
}

@Composable
private fun MealViewSwitch(view: PlanView, onViewChange: (PlanView) -> Unit) = Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
) {
    MealViewLabel(
        text = stringResource(R.string.calendar_view_lunch),
        icon = Icons.Filled.WbSunny,
        isActive = view == PlanView.LUNCH,
        onClick = { onViewChange(PlanView.LUNCH) }
    )
    Switch(
        checked = view == PlanView.DINNER,
        onCheckedChange = { isDinner -> onViewChange(if (isDinner) PlanView.DINNER else PlanView.LUNCH) },
        thumbContent = {
            Icon(
                imageVector = if (view == PlanView.DINNER) Icons.Filled.Bedtime else Icons.Filled.WbSunny,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize)
            )
        },
        colors = SwitchDefaults.colors(
            uncheckedThumbColor = MaterialTheme.colorScheme.primary,
            uncheckedTrackColor = MaterialTheme.colorScheme.primaryContainer,
            uncheckedIconColor = MaterialTheme.colorScheme.onPrimary,
            uncheckedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
    MealViewLabel(
        text = stringResource(R.string.calendar_view_dinner),
        icon = Icons.Filled.Bedtime,
        isActive = view == PlanView.DINNER,
        onClick = { onViewChange(PlanView.DINNER) }
    )
}

@Composable
private fun MealViewLabel(text: String, icon: ImageVector, isActive: Boolean, onClick: () -> Unit) = Row(
    modifier = Modifier
        .clip(RoundedCornerShape(12.dp))
        .clickable(onClick = onClick)
        .padding(horizontal = 8.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp)
) {
    val color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
        color = color
    )
}

@Composable
private fun PlannedLunch.summaryLines(): List<Pair<String, DishSummary>> =
    slots.map { (slot, dish) -> stringResource(slot.labelRes()) to dish }

private fun MenuSlot.labelRes(): Int = when (this) {
    MenuSlot.STARTER -> R.string.calendar_slot_starter
    MenuSlot.MAIN -> R.string.calendar_slot_main
    MenuSlot.SINGLE -> R.string.calendar_slot_single
}

private fun YearMonth.weeks(): List<List<LocalDate?>> {
    val leadingBlanks = atDay(1).dayOfWeek.value - 1
    val cells = List(leadingBlanks) { null } + (1..lengthOfMonth()).map(::atDay)
    return cells.chunked(7) { week -> week + List(7 - week.size) { null } }
}

@Preview(showBackground = true)
@Composable
private fun CalendarScreenPreview() = MenuPlannerTheme {
    val today = LocalDate.of(2026, 10, 9)
    val menu = DailyMenu.Courses(
        starter = PreviewDishes.dish(name = "Gazpacho", type = DishType.STARTER, heaviness = Heaviness.VERY_LOW),
        main = PreviewDishes.dish(name = "Pollo asado", type = DishType.MAIN, heaviness = Heaviness.MEDIUM)
    )
    CalendarScreen(
        state = CalendarUiState(
            month = YearMonth.from(today),
            selectedDate = today,
            today = today,
            isLoading = false,
            days = mapOf(today to MealDay(date = today, menu = menu, updatedAt = Instant.EPOCH).toPlannedLunch())
        ),
        onPreviousMonth = {},
        onNextMonth = {},
        onToday = {},
        onDateSelect = {},
        onOpenDay = {}
    )
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CalendarScreenDarkPreview() = CalendarScreenPreview()
