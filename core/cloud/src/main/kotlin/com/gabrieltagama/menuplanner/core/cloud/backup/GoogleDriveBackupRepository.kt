package com.gabrieltagama.menuplanner.core.cloud.backup

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gabrieltagama.menuplanner.core.cloud.auth.GoogleDriveAuthorizer
import com.gabrieltagama.menuplanner.core.cloud.di.CloudDataStore
import com.gabrieltagama.menuplanner.core.cloud.drive.DriveAppDataClient
import com.gabrieltagama.menuplanner.core.cloud.drive.DriveUnauthorizedException
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.CloudAccount
import com.gabrieltagama.menuplanner.core.domain.port.CloudBackupRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Backup of the recipes in the hidden app data folder of the user's own Google Drive. Only the linked
 * email and the fingerprint of the last upload are kept, in a private DataStore; no password or
 * token is stored. Every operation asks Google for a fresh token silently, so a revoked grant
 * becomes CloudAuthorizationRequired and network problems CloudUnavailable. A new backup is
 * created before the older ones are deleted, so a failed upload never leaves the user without one.
 */
@Singleton
internal class GoogleDriveBackupRepository @Inject constructor(
    @param:CloudDataStore private val dataStore: DataStore<Preferences>,
    private val authorizer: GoogleDriveAuthorizer,
    private val drive: DriveAppDataClient
) : CloudBackupRepository {

    private val preferences: Flow<Preferences> = dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }

    override val account: Flow<CloudAccount> =
        preferences.map { stored -> stored[KEY_EMAIL]?.let(CloudAccount::Connected) ?: CloudAccount.Disconnected }.distinctUntilChanged()

    override suspend fun connect(accessToken: String): Outcome<CloudAccount.Connected> = remote(accessToken) { token ->
        val connected = CloudAccount.Connected(drive.accountEmail(token))
        dataStore.edit {
            it[KEY_EMAIL] = connected.email
            it.remove(KEY_FINGERPRINT)
        }
        connected
    }

    override suspend fun upload(json: String, fingerprint: String): Outcome<Unit> = withDrive { token ->
        val previous = drive.findBackups(token)
        drive.create(token, json)
        previous.forEach { drive.delete(token, it.id) }
        dataStore.edit { it[KEY_FINGERPRINT] = fingerprint }
        Unit
    }

    override suspend fun download(): Outcome<String?> = withDrive { token ->
        drive.findBackups(token).firstOrNull()?.let { drive.download(token, it.id) }
    }

    override suspend fun lastUploadedFingerprint(): String? = preferences.first()[KEY_FINGERPRINT]

    override suspend fun disconnect() {
        connectedEmail()?.let { authorizer.revoke(it) }
        dataStore.edit {
            it.remove(KEY_EMAIL)
            it.remove(KEY_FINGERPRINT)
        }
    }

    private suspend fun connectedEmail(): String? = (account.first() as? CloudAccount.Connected)?.email

    private suspend fun <T> withDrive(block: suspend (String) -> T): Outcome<T> {
        val email = connectedEmail() ?: return Outcome.Failure(DomainError.CloudAuthorizationRequired)
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

    private companion object {
        val KEY_EMAIL = stringPreferencesKey("email")
        val KEY_FINGERPRINT = stringPreferencesKey("last_uploaded_fingerprint")
    }
}
