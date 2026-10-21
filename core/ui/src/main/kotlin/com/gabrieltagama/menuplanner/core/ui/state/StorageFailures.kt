package com.gabrieltagama.menuplanner.core.ui.state

import android.database.SQLException
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.common.StorageError
import java.io.IOException

/**
 * Turns a failure of the device storage (a Room SQLException or a DataStore or file IOException)
 * into StorageError.StorageUnavailable, so a screen shows a message instead of closing the app.
 * runStorageAction does the same for an action that returns nothing. Any other exception,
 * cancellation included, is not caught.
 */
suspend fun <T> catchingStorageFailure(block: suspend () -> Outcome<T>): Outcome<T> =
    try {
        block()
    } catch (expected: SQLException) {
        Outcome.Failure(StorageError.StorageUnavailable)
    } catch (expected: IOException) {
        Outcome.Failure(StorageError.StorageUnavailable)
    }

suspend fun runStorageAction(block: suspend () -> Unit): Outcome<Unit> = catchingStorageFailure { Outcome.Success(block()) }
