package com.gabrieltagama.menuplanner.core.cloud.backup

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.gabrieltagama.menuplanner.core.cloud.auth.GoogleDriveAuthorizer
import com.gabrieltagama.menuplanner.core.cloud.drive.DriveAppDataClient
import com.gabrieltagama.menuplanner.core.cloud.drive.DriveUnauthorizedException
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.CloudAccount
import com.gabrieltagama.menuplanner.core.domain.repository.CloudBackupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Recipe backup in the hidden app data folder of the user's own Google Drive. Only the linked
 * email and the fingerprint of the last upload are kept, in private preferences; no password or
 * token is stored. Every operation asks Google for a fresh token silently, so a revoked grant
 * becomes CloudAuthorizationRequired and network problems CloudUnavailable. A new backup is
 * created before the older ones are deleted, so a failed upload never leaves the user without one.
 */
@Singleton
internal class GoogleDriveBackupRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val authorizer: GoogleDriveAuthorizer,
    private val drive: DriveAppDataClient
) : CloudBackupRepository {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(readAccount())

    override val account: StateFlow<CloudAccount> = state.asStateFlow()

    override suspend fun connect(accessToken: String): Outcome<CloudAccount.Connected> = remote(accessToken) { token ->
        val connected = CloudAccount.Connected(drive.accountEmail(token))
        save {
            putString(KEY_EMAIL, connected.email)
            remove(KEY_FINGERPRINT)
        }
        state.value = connected
        connected
    }

    override suspend fun upload(recipesJson: String, fingerprint: String): Outcome<Unit> = withDrive { token ->
        val previous = drive.findBackups(token)
        drive.create(token, recipesJson)
        previous.forEach { drive.delete(token, it.id) }
        save { putString(KEY_FINGERPRINT, fingerprint) }
    }

    override suspend fun download(): Outcome<String?> = withDrive { token ->
        drive.findBackups(token).firstOrNull()?.let { drive.download(token, it.id) }
    }

    override suspend fun lastUploadedFingerprint(): String? = preferences.getString(KEY_FINGERPRINT, null)

    override suspend fun disconnect() {
        (state.value as? CloudAccount.Connected)?.let { authorizer.revoke(it.email) }
        save {
            remove(KEY_EMAIL)
            remove(KEY_FINGERPRINT)
        }
        state.value = CloudAccount.Disconnected
    }

    private suspend fun <T> withDrive(block: suspend (String) -> T): Outcome<T> {
        val email = (state.value as? CloudAccount.Connected)?.email ?: return Outcome.Failure(DomainError.CloudAuthorizationRequired)
        val token = authorizer.silentToken(email) ?: return Outcome.Failure(DomainError.CloudAuthorizationRequired)
        return remote(token, block)
    }

    private suspend fun <T> remote(token: String, block: suspend (String) -> T): Outcome<T> =
        try {
            Outcome.Success(block(token))
        } catch (exception: DriveUnauthorizedException) {
            authorizer.invalidate(token)
            Outcome.Failure(DomainError.CloudAuthorizationRequired)
        } catch (exception: IOException) {
            Outcome.Failure(DomainError.CloudUnavailable)
        }

    private suspend fun save(change: SharedPreferences.Editor.() -> Unit) =
        withContext(Dispatchers.IO) { preferences.edit(action = change) }

    private fun readAccount(): CloudAccount =
        preferences.getString(KEY_EMAIL, null)?.let(CloudAccount::Connected) ?: CloudAccount.Disconnected

    private companion object {
        const val PREFERENCES_NAME = "menu_planner_cloud_backup"
        const val KEY_EMAIL = "email"
        const val KEY_FINGERPRINT = "last_uploaded_fingerprint"
    }
}
