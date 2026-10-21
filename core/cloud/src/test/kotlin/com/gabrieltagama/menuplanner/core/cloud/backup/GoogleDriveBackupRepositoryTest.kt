package com.gabrieltagama.menuplanner.core.cloud.backup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gabrieltagama.menuplanner.core.cloud.auth.DriveTokenProvider
import com.gabrieltagama.menuplanner.core.cloud.drive.DriveFile
import com.gabrieltagama.menuplanner.core.cloud.drive.DriveFiles
import com.gabrieltagama.menuplanner.core.cloud.drive.DriveUnauthorizedException
import com.gabrieltagama.menuplanner.core.domain.common.CloudError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.CloudAccount
import java.io.File
import java.io.IOException
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Robolectric tests of the Google Drive backup with fake Drive operations and tokens: a new backup
 * is created before the old ones are deleted and a failed upload keeps them, a rejected token or a
 * missing grant means CloudAuthorizationRequired, network problems CloudUnavailable, the newest
 * backup is the one restored, and connecting and disconnecting update the linked account.
 */
@RunWith(AndroidJUnit4::class)
class GoogleDriveBackupRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope) {
        File(context.filesDir, "cloud-test-${UUID.randomUUID()}.preferences_pb")
    }
    private val tokens = FakeTokenProvider()
    private val drive = FakeDriveFiles()
    private val repository = GoogleDriveBackupRepository(dataStore, tokens, drive)

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun connectStoresTheAccountOfTheToken() = runBlocking {
        assertEquals(Outcome.Success(CloudAccount.Connected(EMAIL)), repository.connect(TOKEN))
        assertEquals(CloudAccount.Connected(EMAIL), repository.account.first())
    }

    @Test
    fun uploadCreatesTheNewBackupBeforeDeletingTheOldOnes() = runBlocking {
        repository.connect(TOKEN)
        drive.files += DriveFile("old", Instant.EPOCH)

        assertEquals(Outcome.Success(Unit), repository.upload("{}", "fingerprint"))
        assertEquals(listOf("create", "delete old"), drive.writes)
        assertEquals("fingerprint", repository.lastUploadedFingerprint())
    }

    @Test
    fun failedUploadKeepsTheOldBackupAndTheLastFingerprint() = runBlocking {
        repository.connect(TOKEN)
        drive.files += DriveFile("old", Instant.EPOCH)
        drive.failCreate = IOException("offline")

        assertEquals(Outcome.Failure(CloudError.CloudUnavailable), repository.upload("{}", "fingerprint"))
        assertEquals(emptyList<String>(), drive.writes)
        assertNull(repository.lastUploadedFingerprint())
    }

    @Test
    fun rejectedTokenAsksForAuthorizationAndDropsTheToken() = runBlocking {
        repository.connect(TOKEN)
        drive.failCreate = DriveUnauthorizedException()

        assertEquals(Outcome.Failure(CloudError.CloudAuthorizationRequired), repository.upload("{}", "fingerprint"))
        assertEquals(listOf(TOKEN), tokens.invalidated)
    }

    @Test
    fun missingGrantAsksForAuthorization() = runBlocking {
        repository.connect(TOKEN)
        tokens.silent = null

        assertEquals(Outcome.Failure(CloudError.CloudAuthorizationRequired), repository.download())
    }

    @Test
    fun disconnectedAccountAsksForAuthorization() = runBlocking {
        assertEquals(Outcome.Failure(CloudError.CloudAuthorizationRequired), repository.download())
    }

    @Test
    fun downloadRestoresTheNewestBackup() = runBlocking {
        repository.connect(TOKEN)
        drive.files += DriveFile("newest", Instant.parse("2026-10-20T10:00:00Z"))
        drive.files += DriveFile("older", Instant.parse("2026-10-01T10:00:00Z"))

        assertEquals(Outcome.Success("content of newest"), repository.download())
    }

    @Test
    fun downloadWithoutBackupReturnsNothing() = runBlocking {
        repository.connect(TOKEN)

        assertEquals(Outcome.Success(null), repository.download())
    }

    @Test
    fun disconnectRevokesTheGrantAndForgetsTheAccount() = runBlocking {
        repository.connect(TOKEN)
        repository.upload("{}", "fingerprint")

        repository.disconnect()

        assertEquals(listOf(EMAIL), tokens.revoked)
        assertEquals(CloudAccount.Disconnected, repository.account.first())
        assertNull(repository.lastUploadedFingerprint())
    }

    private class FakeTokenProvider : DriveTokenProvider {
        var silent: String? = TOKEN
        val invalidated = mutableListOf<String>()
        val revoked = mutableListOf<String>()

        override suspend fun silentToken(email: String): String? = silent

        override suspend fun invalidate(token: String) {
            invalidated += token
        }

        override suspend fun revoke(email: String) {
            revoked += email
        }
    }

    private class FakeDriveFiles : DriveFiles {
        val files = mutableListOf<DriveFile>()
        val writes = mutableListOf<String>()
        var failCreate: IOException? = null

        override suspend fun accountEmail(token: String): String = EMAIL

        override suspend fun findBackups(token: String): List<DriveFile> = files.sortedByDescending(DriveFile::modifiedTime)

        override suspend fun download(token: String, fileId: String): String = "content of $fileId"

        override suspend fun create(token: String, content: String) {
            failCreate?.let { throw it }
            writes += "create"
        }

        override suspend fun delete(token: String, fileId: String) {
            writes += "delete $fileId"
        }
    }

    private companion object {
        const val EMAIL = "user@example.com"
        const val TOKEN = "token"
    }
}
