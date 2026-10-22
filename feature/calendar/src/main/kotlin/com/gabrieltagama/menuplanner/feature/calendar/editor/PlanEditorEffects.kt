package com.gabrieltagama.menuplanner.feature.calendar.editor

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.ui.component.SnackbarMessageEffect
import com.gabrieltagama.menuplanner.core.ui.text.message

/**
 * Side effects shared by the lunch and dinner editor routes: closes the editor when the ViewModel
 * sends PlanEditorEvent.Close and shows the current error in the snackbar once, then marks it shown.
 */
@Composable
internal fun PlanEditorEffects(
    viewModel: PlanEditorViewModel<*, *>,
    error: DomainError?,
    snackbarHostState: SnackbarHostState,
    onClose: () -> Unit
) {
    val currentOnClose by rememberUpdatedState(onClose)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                PlanEditorEvent.Close -> currentOnClose()
            }
        }
    }
    SnackbarMessageEffect(message = error?.message(), snackbarHostState = snackbarHostState, onShown = viewModel::onErrorShown)
}
