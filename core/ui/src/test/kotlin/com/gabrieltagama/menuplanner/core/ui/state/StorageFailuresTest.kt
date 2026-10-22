package com.gabrieltagama.menuplanner.core.ui.state

import android.database.sqlite.SQLiteException
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.common.StorageError
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests that storage failures (Room and DataStore) become StorageUnavailable, that results pass
 * through unchanged and that other exceptions are not swallowed.
 */
@RunWith(AndroidJUnit4::class)
class StorageFailuresTest {
    private val unavailable = Outcome.Failure(StorageError.StorageUnavailable)

    @Test
    fun databaseFailureBecomesStorageUnavailable() = runBlocking {
        assertEquals(unavailable, catchingStorageFailure<Unit> { throw SQLiteException("disk I/O error") })
    }

    @Test
    fun fileFailureBecomesStorageUnavailable() = runBlocking {
        assertEquals(unavailable, runStorageAction { throw IOException("disk full") })
    }

    @Test
    fun resultsPassThrough() = runBlocking {
        assertEquals(Outcome.Success(3), catchingStorageFailure { Outcome.Success(3) })
        assertEquals(Outcome.Success(Unit), runStorageAction { })
    }

    @Test(expected = IllegalStateException::class)
    fun otherExceptionsAreNotCaught() {
        runBlocking { runStorageAction { error("bug") } }
    }
}
