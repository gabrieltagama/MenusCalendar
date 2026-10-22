package com.gabrieltagama.menuplanner.core.ui.component

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * Shows a one-shot message of a screen in its snackbar and then calls onShown, so the ViewModel
 * clears it and the same message is not shown again after a recomposition. A null message shows
 * nothing.
 */
@Composable
fun SnackbarMessageEffect(message: String?, snackbarHostState: SnackbarHostState, onShown: () -> Unit) {
    val currentOnShown by rememberUpdatedState(onShown)
    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        currentOnShown()
    }
}
