package com.gabrieltagama.menuplanner.core.ui.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * How every screen shares its state: the upstream flows run while the screen is collecting and for
 * STOP_TIMEOUT_MILLIS after it stops, so a configuration change does not restart them.
 */
fun <T> Flow<T>.stateWhileSubscribed(scope: CoroutineScope, initialValue: T): StateFlow<T> =
    stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), initialValue)

private const val STOP_TIMEOUT_MILLIS = 5_000L
