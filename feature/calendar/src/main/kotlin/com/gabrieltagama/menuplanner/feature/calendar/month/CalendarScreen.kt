package com.gabrieltagama.menuplanner.feature.calendar.month

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.ui.component.ConfirmDialog
import com.gabrieltagama.menuplanner.core.ui.theme.MenuPlannerTheme
import com.gabrieltagama.menuplanner.feature.calendar.R
import com.gabrieltagama.menuplanner.feature.calendar.common.inlineText
import com.gabrieltagama.menuplanner.feature.calendar.common.longTitleText
import com.gabrieltagama.menuplanner.feature.calendar.common.titleText
import java.time.LocalDate
import java.time.YearMonth

/**
 * Monthly calendar: stateful route bound to [CalendarViewModel] and a stateless screen with a
 * Monday-first month grid and the summary of the selected day. The top bar also offers the
 * random autofill of the visible month, confirmed with a dialog and summarised in a snackbar.
 * Bottom insets are left to the app shell, which draws the NavigationBar below this screen.
 */
data class AutoFillActions(
    val onRequest: () -> Unit = {},
    val onConfirm: () -> Unit = {},
    val onDismiss: () -> Unit = {}
)

@Composable
fun CalendarScreenRoute(
    onOpenDay: (LocalDate) -> Unit,
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
    autoFillActions: AutoFillActions = AutoFillActions(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) = Scaffold(
    contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
    topBar = {
        TopAppBar(
            title = { Text(state.month.titleText()) },
            actions = {
                IconButton(onClick = autoFillActions.onRequest, enabled = !state.isAutoFilling && !state.isLoading) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = stringResource(R.string.calendar_autofill))
                }
                IconButton(onClick = onPreviousMonth) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.calendar_previous_month))
                }
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.calendar_next_month))
                }
                TextButton(onClick = onToday) { Text(stringResource(R.string.calendar_today)) }
            }
        )
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
        modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(4.dp)) {
            if (state.isLoading || state.isAutoFilling) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        WeekdayHeader()
        MonthGrid(state = state, onDateSelect = onDateSelect)
        DaySummaryCard(
            date = state.selectedDate,
            day = state.selectedDay,
            onOpenDay = { onOpenDay(state.selectedDate) },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun WeekdayHeader() = Row(modifier = Modifier.fillMaxWidth()) {
    stringArrayResource(R.array.calendar_weekday_initials).forEach { initial ->
        Text(
            text = initial,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MonthGrid(state: CalendarUiState, onDateSelect: (LocalDate) -> Unit) = Column(
    verticalArrangement = Arrangement.spacedBy(4.dp)
) {
    state.month.weeks().forEach { week ->
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
        isSelected -> colors.primaryContainer
        isToday -> colors.secondaryContainer
        else -> Color.Transparent
    }
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(shape)
            .background(background)
            .border(width = if (isToday) 1.5.dp else 0.dp, color = if (isToday) colors.primary else Color.Transparent, shape = shape)
            .clickable(onClick = onClick)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday) colors.primary else colors.onSurface
            )
            if (day != null) Box(modifier = Modifier.padding(start = 2.dp).size(5.dp).background(colors.tertiary, CircleShape))
        }
        day?.let {
            Text(
                text = it.menu.dishes.joinToString(" · ") { dish -> dish.name },
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 10.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DaySummaryCard(date: LocalDate, day: MealDay?, onOpenDay: () -> Unit, modifier: Modifier = Modifier) =
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = date.longTitleText().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium
            )
            if (day == null) {
                Text(text = stringResource(R.string.calendar_no_menu), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onOpenDay) { Text(stringResource(R.string.calendar_plan)) }
            } else {
                day.menu.summaryLines().forEach { (label, dish) -> SummaryLine(label = label, dishName = dish?.name) }
                Button(onClick = onOpenDay) { Text(stringResource(R.string.calendar_edit)) }
            }
        }
    }

@Composable
private fun SummaryLine(label: String, dishName: String?) = Row {
    Text(
        text = "$label: ",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold
    )
    Text(text = dishName ?: stringResource(R.string.calendar_no_dessert), style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun DailyMenu.summaryLines(): List<Pair<String, Dish?>> = when (this) {
    is DailyMenu.Courses -> listOf(
        stringResource(R.string.calendar_slot_starter) to starter,
        stringResource(R.string.calendar_slot_main) to main,
        stringResource(R.string.calendar_slot_dessert) to dessert
    )
    is DailyMenu.Single -> listOf(
        stringResource(R.string.calendar_slot_single) to single,
        stringResource(R.string.calendar_slot_dessert) to dessert
    )
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
