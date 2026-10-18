package com.gabrieltagama.menuplanner.feature.calendar.month

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.gabrieltagama.menuplanner.core.domain.model.plan.DishSummary
import com.gabrieltagama.menuplanner.core.ui.component.HeavinessDot
import com.gabrieltagama.menuplanner.feature.calendar.R
import java.time.LocalDate
import java.time.YearMonth

/**
 * Month card of the calendar: Monday-first weekday header and an animated grid of the active view
 * with one heaviness dot per planned dish. Swiping left/right changes month, except while marking
 * days. A tap selects the day, toggles it in selection mode or pastes a copy on it; a long press
 * (or any drag while selecting) marks the continuous range under the finger.
 */
@Composable
internal fun MonthCard(
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

private fun CalendarUiState.plannedDishes(date: LocalDate): List<DishSummary> = when (view) {
    PlanView.LUNCH -> days[date]?.dishes.orEmpty()
    PlanView.DINNER -> listOfNotNull(dinners[date]?.dish)
}

private fun YearMonth.weeks(): List<List<LocalDate?>> {
    val leadingBlanks = atDay(1).dayOfWeek.value - 1
    val cells = List(leadingBlanks) { null } + (1..lengthOfMonth()).map(::atDay)
    return cells.chunked(7) { week -> week + List(7 - week.size) { null } }
}

private const val MaxDotsPerDay = 3
private val CellHeight = 56.dp
private val GridGap = 4.dp
