package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.repository.BackupScheduler

/**
 * BackupScheduler that only records whether the daily backup is scheduled.
 */
class FakeBackupScheduler : BackupScheduler {
    var isScheduled = false
        private set

    override fun scheduleDaily() {
        isScheduled = true
    }

    override fun cancel() {
        isScheduled = false
    }
}
