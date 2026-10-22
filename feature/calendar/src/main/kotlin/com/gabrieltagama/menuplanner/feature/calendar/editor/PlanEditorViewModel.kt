package com.gabrieltagama.menuplanner.feature.calendar.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.common.fold
import com.gabrieltagama.menuplanner.core.domain.model.PlannedDay
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.DayPlanCommands
import com.gabrieltagama.menuplanner.core.ui.state.catchingStorageFailure
import com.gabrieltagama.menuplanner.core.ui.state.runStorageAction
import java.time.LocalDate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Shared behaviour of the day editors (lunch and dinner) over the DayPlanCommands of their meal:
 * loading the saved day of [date], the clear confirmation, error dismissal and the save and clear
 * flows, which close the editor when they finish or show the error (a business rule or a storage
 * failure) and keep it open. All of that progress lives in one EditorStatus; subclasses keep only
 * their form, say how it is filled from the saved day and save it.
 */
abstract class PlanEditorViewModel<D : PlannedDay<D>, F>(
    protected val date: LocalDate,
    private val commands: DayPlanCommands<D>,
    initialForm: F
) : ViewModel() {

    protected val form = MutableStateFlow(initialForm)

    protected val status = MutableStateFlow(EditorStatus())

    private val _events = Channel<PlanEditorEvent>(Channel.BUFFERED)
    val events: Flow<PlanEditorEvent> = _events.receiveAsFlow()

    protected abstract fun F.filledWith(day: D): F

    fun onClearRequest() = status.update { it.copy(showClearConfirm = true) }

    fun onClearDismiss() = status.update { it.copy(showClearConfirm = false) }

    fun onErrorShown() = status.update { it.copy(error = null) }

    fun onClearConfirm() {
        status.update { it.copy(showClearConfirm = false, isSaving = true) }
        finish { runStorageAction { commands.clearDay(date) } }
    }

    protected fun loadSavedDay() {
        viewModelScope.launch {
            catchingStorageFailure { Outcome.Success(commands.getDay(date)) }.fold(
                onSuccess = { day ->
                    day?.let { saved -> form.update { it.filledWith(saved) } }
                    status.update { it.copy(isLoading = false, hasSavedPlan = day != null) }
                },
                onFailure = { error -> status.update { it.copy(isLoading = false, error = error) } }
            )
        }
    }

    protected fun save(action: suspend () -> Outcome<*>) {
        status.update { it.copy(isSaving = true) }
        finish { catchingStorageFailure { action() } }
    }

    private fun finish(action: suspend () -> Outcome<*>) {
        viewModelScope.launch {
            action().fold(
                onSuccess = { _events.send(PlanEditorEvent.Close) },
                onFailure = { error -> status.update { it.copy(isSaving = false, error = error) } }
            )
        }
    }
}
