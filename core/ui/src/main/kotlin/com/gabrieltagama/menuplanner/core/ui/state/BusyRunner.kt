package com.gabrieltagama.menuplanner.core.ui.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Runs one long operation of a screen at a time in [scope]: a new request while another is running
 * is ignored. onBusyChange reports true when an operation starts and false when it ends, also when
 * it fails or is cancelled, so the busy flag of the screen is always released.
 */
class BusyRunner(private val scope: CoroutineScope, private val onBusyChange: (Boolean) -> Unit) {
    private var job: Job? = null

    val isRunning: Boolean get() = job?.isActive == true

    fun run(block: suspend () -> Unit) {
        if (isRunning) return
        job = scope.launch {
            onBusyChange(true)
            try {
                block()
            } finally {
                onBusyChange(false)
            }
        }
    }
}
