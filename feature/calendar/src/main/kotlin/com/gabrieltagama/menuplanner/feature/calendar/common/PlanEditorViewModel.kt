package com.gabrieltagama.menuplanner.feature.calendar.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import kotlin.random.Random
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Behaviour shared by the editors of one calendar day (lunch and dinner): the form status
 * (loading, saving, clear confirmation, error), saving or clearing the day and then emitting
 * closeEvent, and picking a random dish different from the current one when possible. Subclasses
 * keep their own selection in the form and build the day to save.
 */
interface EditorForm<F : EditorForm<F>> {
    val isLoading: Boolean
    val isSaving: Boolean
    val showClearConfirm: Boolean
    val error: DomainError?

    fun withStatus(isSaving: Boolean = this.isSaving, showClearConfirm: Boolean = this.showClearConfirm, error: DomainError? = this.error): F
}

abstract class PlanEditorViewModel<F : EditorForm<F>, E>(
    initialForm: F,
    private val closeEvent: E,
    private val random: Random
) : ViewModel() {

    protected val form = MutableStateFlow(initialForm)

    private val _events = Channel<E>(Channel.BUFFERED)
    val events: Flow<E> = _events.receiveAsFlow()

    protected abstract suspend fun clearDay()

    fun onClearRequest() = form.update { it.withStatus(showClearConfirm = true) }

    fun onClearDismiss() = form.update { it.withStatus(showClearConfirm = false) }

    fun onErrorShown() = form.update { it.withStatus(error = null) }

    fun onClearConfirm() {
        form.update { it.withStatus(showClearConfirm = false, isSaving = true) }
        viewModelScope.launch {
            clearDay()
            _events.send(closeEvent)
        }
    }

    protected fun saveDay(save: suspend () -> Outcome<*>) {
        form.update { it.withStatus(isSaving = true) }
        viewModelScope.launch {
            when (val outcome = save()) {
                is Outcome.Success -> _events.send(closeEvent)
                is Outcome.Failure -> form.update { it.withStatus(isSaving = false, error = outcome.error) }
            }
        }
    }

    protected fun randomDish(options: List<Dish>, current: Dish?): Dish? =
        options.filterNot { it.id == current?.id }.ifEmpty { options }.randomOrNull(random)
}
