package com.gabrieltagama.menuplanner.feature.calendar.month

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.model.plan.toPlannedLunch
import com.gabrieltagama.menuplanner.core.ui.component.ConfirmDialog
import com.gabrieltagama.menuplanner.core.ui.preview.PreviewDishes
import com.gabrieltagama.menuplanner.core.ui.theme.MenuPlannerTheme
import com.gabrieltagama.menuplanner.feature.calendar.R
import com.gabrieltagama.menuplanner.feature.calendar.common.inlineText
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/**
 * Monthly calendar: stateful route bound to [CalendarViewModel] and a stateless screen that lays
 * out the parts kept in their own files: the top bar of the current mode (CalendarTopBar), the
 * lunches/dinners switch (MealViewSwitch), the month grid (MonthCard), the summary of the selected
 * day (DaySummaryCard), the selection actions (SelectionActionBar) and the snackbars of the
 * one-shot events (CalendarEventsEffect). The random autofill of the active view for the visible
 * month is a FAB confirmed with a dialog; below the day summary a button opens the shopping list of
 * the visible month. Back leaves the selection or paste mode. Bottom insets are left to the app
 * shell, which draws the NavigationBar below this screen.
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

private val FabClearance = 96.dp

@Composable
fun CalendarScreenRoute(
    onOpenDay: (LocalDate) -> Unit,
    onOpenDinner: (LocalDate) -> Unit,
    onOpenShoppingList: (YearMonth) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LifecycleResumeEffect(viewModel) {
        viewModel.onScreenResume()
        onPauseOrDispose { }
    }

    CalendarEventsEffect(
        events = viewModel.events,
        snackbarHostState = snackbarHostState,
        onUndo = viewModel::onUndo
    )

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

@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onDateSelect: (LocalDate) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onOpenDinner: (LocalDate) -> Unit = {},
    onViewChange: (Meal) -> Unit = {},
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
            CalendarTopBar(state = state, onToday = onToday, onPreviousMonth = onPreviousMonth, onNextMonth = onNextMonth, selectionActions = selectionActions)
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
                onOpen = { if (state.view == Meal.LUNCH) onOpenDay(state.selectedDate) else onOpenDinner(state.selectedDate) }
            )
            OutlinedButton(onClick = onOpenShoppingList, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.ShoppingCart, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.calendar_shopping_list))
            }
        }
    }
}

@Composable
private fun AutoFillFab(scrollState: ScrollState, onClick: () -> Unit) = ExtendedFloatingActionButton(
    text = { Text(stringResource(R.string.calendar_autofill_short)) },
    icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = stringResource(R.string.calendar_autofill)) },
    onClick = onClick,
    expanded = scrollState.value == 0
)

@Composable
private fun CalendarUiState.autoFillPeriodText(): String =
    if (isShowingCurrentMonth) stringResource(R.string.calendar_autofill_from_today, month.inlineText()) else month.inlineText()

private fun Meal.autoFillTextRes(): Int = when (this) {
    Meal.LUNCH -> R.string.calendar_autofill_text
    Meal.DINNER -> R.string.calendar_autofill_dinner_text
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
