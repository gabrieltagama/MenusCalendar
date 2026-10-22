package com.gabrieltagama.menuplanner.feature.calendar.month

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.gabrieltagama.menuplanner.core.domain.common.StorageError
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.UndoableChange
import com.gabrieltagama.menuplanner.core.ui.text.message
import com.gabrieltagama.menuplanner.feature.calendar.R
import kotlinx.coroutines.flow.Flow

/**
 * Shows every one-shot CalendarEvent in the snackbar: autofill summaries (AutoFillMessageFormatter),
 * "nothing to do" notices, storage failures, and deletions and pastes with a Deshacer action that
 * calls back with what changed so it can be reverted.
 */
@Composable
internal fun CalendarEventsEffect(
    events: Flow<CalendarEvent>,
    snackbarHostState: SnackbarHostState,
    onUndo: (UndoableChange) -> Unit
) {
    val resources = LocalResources.current
    val formatter = remember(resources) { AutoFillMessageFormatter(ResourcesAutoFillTexts(resources)) }
    val undoLabel = stringResource(R.string.calendar_undo)
    val storageFailedText = StorageError.StorageUnavailable.message()
    val currentOnUndo by rememberUpdatedState(onUndo)

    suspend fun showUndoable(change: UndoableChange, textRes: Int) {
        val message = resources.getQuantityString(textRes, change.count, change.count)
        val result = snackbarHostState.showSnackbar(message, actionLabel = undoLabel, duration = SnackbarDuration.Long)
        if (result == SnackbarResult.ActionPerformed) currentOnUndo(change)
    }

    LaunchedEffect(events, formatter) {
        events.collect { event ->
            when (event) {
                is CalendarEvent.AutoFillCompleted -> snackbarHostState.showSnackbar(formatter.format(event.result, event.view))
                is CalendarEvent.DaysDeleted -> showUndoable(event.change, event.change.view.deletedTextRes())
                is CalendarEvent.DaysPasted -> showUndoable(event.change, event.change.view.pastedTextRes())
                is CalendarEvent.SelectionFilled -> snackbarHostState.showSnackbar(
                    if (event.result.filledDays == 0 && event.result.unfilledDays == 0) resources.getString(R.string.calendar_selection_nothing_to_fill)
                    else formatter.format(event.result, event.view)
                )
                CalendarEvent.NothingPlanned -> snackbarHostState.showSnackbar(resources.getString(R.string.calendar_selection_nothing_planned))
                CalendarEvent.StorageFailed -> snackbarHostState.showSnackbar(storageFailedText)
            }
        }
    }
}

private fun Meal.deletedTextRes(): Int = when (this) {
    Meal.LUNCH -> R.plurals.calendar_deleted_lunches
    Meal.DINNER -> R.plurals.calendar_deleted_dinners
}

private fun Meal.pastedTextRes(): Int = when (this) {
    Meal.LUNCH -> R.plurals.calendar_pasted_lunches
    Meal.DINNER -> R.plurals.calendar_pasted_dinners
}
