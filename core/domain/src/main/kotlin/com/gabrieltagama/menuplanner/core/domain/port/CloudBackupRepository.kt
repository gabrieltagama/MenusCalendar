package com.gabrieltagama.menuplanner.core.domain.port

import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.CloudAccount
import kotlinx.coroutines.flow.Flow

/**
 * Port of the personal cloud backup of the recipe book and the calendar. Data lives only in the user's own
 * storage, there is no server of the app, so nobody else can see the account or the data.
 * connect receives an access token already granted by the user. Implemented in :core:cloud.
 */
interface CloudBackupRepository {
    val account: Flow<CloudAccount>
    suspend fun connect(accessToken: String): Outcome<CloudAccount.Connected>
    suspend fun upload(json: String, fingerprint: String): Outcome<Unit>
    suspend fun download(): Outcome<String?>
    suspend fun lastUploadedFingerprint(): String?
    suspend fun disconnect()
}
