package com.gabrieltagama.menuplanner.core.domain.repository

import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.CloudAccount
import kotlinx.coroutines.flow.StateFlow

/**
 * Port of the personal cloud backup of the recipe book. Data lives only in the user's own
 * storage, there is no server of the app, so nobody else can see the account or the recipes.
 * connect receives an access token already granted by the user. Implemented in :core:cloud.
 */
interface CloudBackupRepository {
    val account: StateFlow<CloudAccount>
    suspend fun connect(accessToken: String): Outcome<CloudAccount.Connected>
    suspend fun upload(recipesJson: String, fingerprint: String): Outcome<Unit>
    suspend fun download(): Outcome<String?>
    suspend fun lastUploadedFingerprint(): String?
    suspend fun disconnect()
}
