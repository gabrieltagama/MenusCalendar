package com.gabrieltagama.menuplanner.core.domain.repository

/**
 * Port that runs the automatic daily recipe backup in the background. Implemented in :core:cloud.
 */
interface BackupScheduler {
    fun scheduleDaily()
    fun cancel()
}
