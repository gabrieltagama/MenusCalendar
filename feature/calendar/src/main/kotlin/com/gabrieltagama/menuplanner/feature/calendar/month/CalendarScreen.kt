package com.gabrieltagama.menuplanner.feature.calendar.month

import android.content.res.Configuration
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.ui.component.ConfirmDialog
import com.gabrieltagama.menuplanner.core.ui.component.DishTypeAvatar
import com.gabrieltagama.menuplanner.core.ui.component.HeavinessChip
import com.gabrieltagama.menuplanner.core.ui.component.HeavinessDot
import com.gabrieltagama.menuplanner.core.ui.theme.MenuPlannerTheme
import com.gabrieltagama.menuplanner.feature.calendar.R
import com.gabrieltagama.menuplanner.feature.calendar.common.inlineText
import com.gabrieltagama.menuplanner.feature.calendar.common.longTitleText
import com.gabrieltagama.menuplanner.feature.calendar.common.titleText
import java.time.LocalDate
import java.time.YearMonth

/**
 * Monthly calendar: stateful route bound to [CalendarViewModel] and a stateless screen with a
 * Monday-first month grid (swipe left/right to change month, one heaviness dot per planned
 * dish) and an animated summary card of the selected day. The random autofill of the visible
 * month is a FAB confirmed with a dialog and summarised in a snackbar; "Hoy" only appears when
 * another month is shown. Below the day summary a button opens the shopping list of the visible
 * month. Bottom insets are left to the app shell, which draws the
 * NavigationBar below this screen.
 */
data class AutoFillActions(
    val onRequest: () -> Unit = {},
    val onConfirm: () -> Unit = {},
    val onDismiss: () -> Unit = {}
)

private const val MaxDotsPerDay = 3
private val FabClearance = 96.dp

@Composable
fun CalendarScreenRoute(
    onOpenDay: (LocalDate) -> Unit,
    onOpenShoppingList: (YearMonth) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val formatter = remember(resources) { AutoFillMessageFormatter(ResourcesAutoFillTexts(resources)) }

    LaunchedEffect(viewModel, formatter) {
        viewModel.events.collect { event ->
            when (event) {
                is CalendarEvent.AutoFillCompleted -> snackbarHostState.showSnackbar(formatter.format(event.result))
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
        onOpenShoppingList = { onOpenShoppingList(state.month) },
        autoFillActions = AutoFillActions(
            onRequest = viewModel::onAutoFillRequest,
            onConfirm = viewModel::onAutoFill,
            onDismiss = viewModel::onAutoFillDismiss
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
    onOpenShoppingList: () -> Unit = {},
    autoFillActions: AutoFillActions = AutoFillActions(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val scrollState = rememberScrollState()
    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
        topBar = {
            TopAppBar(
                title = { Text(state.month.titleText()) },
                actions = {
                    if (!state.isShowingCurrentMonth) TextButton(onClick = onToday) { Text(stringResource(R.string.calendar_today)) }
                    IconButton(onClick = onPreviousMonth) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.calendar_previous_month))
                    }
                    IconButton(onClick = onNextMonth) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.calendar_next_month))
                    }
                }
            )
        },
        floatingActionButton = {
            if (!state.isLoading && !state.isAutoFilling) AutoFillFab(scrollState = scrollState, onClick = autoFillActions.onRequest)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.showAutoFillConfirm)
            ConfirmDialog(
                title = stringResource(R.string.calendar_autofill_title),
                text = stringResource(R.string.calendar_autofill_text, state.month.inlineText()),
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
            MonthCard(state = state, onDateSelect = onDateSelect, onPreviousMonth = onPreviousMonth, onNextMonth = onNextMonth)
            DaySummaryCard(date = state.selectedDate, day = state.selectedDay, onOpenDay = { onOpenDay(state.selectedDate) })
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
private fun MonthCard(
    state: CalendarUiState,
    onDateSelect: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) = Surface(
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    modifier = Modifier.fillMaxWidth().monthSwipe(onPreviousMonth = onPreviousMonth, onNextMonth = onNextMonth)
) {
    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        WeekdayHeader()
        AnimatedContent(targetState = state.month, transitionSpec = { monthTransition() }, label = "month") { month ->
            MonthGrid(month = month, state = state, onDateSelect = onDateSelect)
        }
    }
}

@Composable
private fun Modifier.monthSwipe(onPreviousMonth: () -> Unit, onNextMonth: () -> Unit): Modifier {
    val currentOnPrevious by rememberUpdatedState(onPreviousMonth)
    val currentOnNext by rememberUpdatedState(onNextMonth)
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
private fun MonthGrid(month: YearMonth, state: CalendarUiState, onDateSelect: (LocalDate) -> Unit) = Column(
    verticalArrangement = Arrangement.spacedBy(4.dp)
) {
    month.weeks().forEach { week ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            week.forEach { date ->
                Box(modifier = Modifier.weight(1f)) {
                    if (date != null)
                        DayCell(
                            date = date,
                            day = state.days[date],
                            isToday = date == state.today,
                            isSelected = date == state.selectedDate,
                            onClick = { onDateSelect(date) }
                        )
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, day: MealDay?, isToday: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val background = when {
        isSelected -> colors.primary
        isToday -> colors.primaryContainer
        day != null -> colors.surfaceContainerHighest
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> colors.onPrimary
        isToday -> colors.onPrimaryContainer
        else -> colors.onSurface
    }
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
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
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.height(6.dp)) {
            day?.menu?.dishes?.take(MaxDotsPerDay)?.forEach { HeavinessDot(heaviness = it.heaviness, size = 6.dp) }
        }
    }
}

@Composable
private fun DaySummaryCard(date: LocalDate, day: MealDay?, onOpenDay: () -> Unit) = Surface(
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surfaceContainer,
    modifier = Modifier.fillMaxWidth()
) {
    AnimatedContent(targetState = date to day, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "day") { (shownDate, shownDay) ->
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = shownDate.longTitleText().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleLarge
            )
            if (shownDay == null) EmptyDaySummary(onOpenDay = onOpenDay)
            else PlannedDaySummary(menu = shownDay.menu, onOpenDay = onOpenDay)
        }
    }
}

@Composable
private fun EmptyDaySummary(onOpenDay: () -> Unit) = Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Filled.EventBusy, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = stringResource(R.string.calendar_no_menu),
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
private fun PlannedDaySummary(menu: DailyMenu, onOpenDay: () -> Unit) = Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    menu.summaryLines().forEach { (label, dish) -> SummaryLine(label = label, dish = dish) }
    OutlinedButton(onClick = onOpenDay, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.size(8.dp))
        Text(stringResource(R.string.calendar_edit))
    }
}

@Composable
private fun SummaryLine(label: String, dish: Dish) = Row(
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
private fun DailyMenu.summaryLines(): List<Pair<String, Dish>> = when (this) {
    is DailyMenu.Courses -> listOf(
        stringResource(R.string.calendar_slot_starter) to starter,
        stringResource(R.string.calendar_slot_main) to main,
        stringResource(R.string.calendar_slot_dessert) to dessert
    )
    is DailyMenu.Single -> listOf(
        stringResource(R.string.calendar_slot_single) to single,
        stringResource(R.string.calendar_slot_dessert) to dessert
    )
}.mapNotNull { (label, dish) -> dish?.let { label to it } }

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
        starter = Dish(name = "Gazpacho", type = DishType.STARTER, heaviness = Heaviness.VERY_LOW),
        main = Dish(name = "Pollo asado", type = DishType.MAIN, heaviness = Heaviness.MEDIUM)
    )
    CalendarScreen(
        state = CalendarUiState(
            month = YearMonth.from(today),
            selectedDate = today,
            today = today,
            isLoading = false,
            days = mapOf(today to MealDay(date = today, menu = menu))
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
