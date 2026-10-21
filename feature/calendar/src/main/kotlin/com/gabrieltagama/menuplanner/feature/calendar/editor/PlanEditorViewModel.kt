package com.gabrieltagama.menuplanner.feature.calendar.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.common.fold
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Shared behaviour of the day editors (lunch and dinner): clear confirmation, error dismissal and
 * the save/clear flows that close the editor when they finish. Subclasses only describe how their
 * form reflects each status change and how the day is saved or cleared.
 */
sealed interface PlanEditorEvent {
    data object Close : PlanEditorEvent
}

abstract class PlanEditorViewModel<F>(initialForm: F) : ViewModel() {

    protected val form = MutableStateFlow(initialForm)

    private val _events = Channel<PlanEditorEvent>(Channel.BUFFERED)
    val events: Flow<PlanEditorEvent> = _events.receiveAsFlow()

    protected abstract fun F.withClearConfirm(visible: Boolean): F

    protected abstract fun F.withSaving(saving: Boolean): F

    protected abstract fun F.withError(error: DomainError?): F

    protected abstract suspend fun clearDay()

    fun onClearRequest() = form.update { it.withClearConfirm(true) }

    fun onClearDismiss() = form.update { it.withClearConfirm(false) }

    fun onErrorShown() = form.update { it.withError(null) }

    fun onClearConfirm() {
        form.update { it.withClearConfirm(false).withSaving(true) }
        viewModelScope.launch {
            clearDay()
            _events.send(PlanEditorEvent.Close)
        }
    }

    protected fun save(action: suspend () -> Outcome<*>) {
        form.update { it.withSaving(true) }
        viewModelScope.launch {
            action().fold(
                onSuccess = { _events.send(PlanEditorEvent.Close) },
                onFailure = { error -> form.update { it.withSaving(false).withError(error) } }
            )
        }
    }
}
