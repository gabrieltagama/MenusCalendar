package com.gabrieltagama.menuplanner.feature.calendar.month

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.gabrieltagama.menuplanner.feature.calendar.R
import com.gabrieltagama.menuplanner.feature.calendar.common.titleText

/**
 * Top bar of the calendar for each CalendarMode: the month title with "Hoy", the selection button
 * and the month arrows while browsing; a contextual bar with the marked count, select all and
 * cancel while selecting; and the paste hint with cancel while a copy waits for its target day.
 */
@Composable
internal fun CalendarTopBar(
    state: CalendarUiState,
    onToday: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    selectionActions: SelectionActions
) = when (val mode = state.mode) {
    is CalendarMode.Pasting -> PasteTopBar(count = mode.clipboard.count, onPreviousMonth = onPreviousMonth, onNextMonth = onNextMonth, onCancel = selectionActions.onPasteCancel)
    is CalendarMode.Selecting -> SelectionTopBar(count = mode.selection.count, onPreviousMonth = onPreviousMonth, onNextMonth = onNextMonth, actions = selectionActions)
    CalendarMode.Browsing -> MonthTopBar(state = state, onToday = onToday, onPreviousMonth = onPreviousMonth, onNextMonth = onNextMonth, onStartSelection = selectionActions.onStart)
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
private fun MonthArrows(onPreviousMonth: () -> Unit, onNextMonth: () -> Unit) {
    IconButton(onClick = onPreviousMonth) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.calendar_previous_month))
    }
    IconButton(onClick = onNextMonth) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.calendar_next_month))
    }
}
