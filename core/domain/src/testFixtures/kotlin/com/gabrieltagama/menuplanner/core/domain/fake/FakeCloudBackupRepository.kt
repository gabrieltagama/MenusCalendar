package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.CloudAccount
import com.gabrieltagama.menuplanner.core.domain.repository.CloudBackupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow

/**
 * In-memory CloudBackupRepository: keeps the remote backup as a string and records uploads;
 * failure, when set, is returned by every remote operation.
 */
class FakeCloudBackupRepository(
    initial: CloudAccount = CloudAccount.Disconnected,
    var remoteJson: String? = null,
    var failure: Outcome.Failure? = null
) : CloudBackupRepository {
    private val state = MutableStateFlow(initial)
    private var fingerprint: String? = null
    val uploads = mutableListOf<String>()

    override val account: Flow<CloudAccount> = state

    override suspend fun connect(accessToken: String): Outcome<CloudAccount.Connected> {
        failure?.let { return it }
        val connected = CloudAccount.Connected(EMAIL)
        state.value = connected
        return Outcome.Success(connected)
    }

    override suspend fun upload(json: String, fingerprint: String): Outcome<Unit> {
        failure?.let { return it }
        uploads += json
        remoteJson = json
        this.fingerprint = fingerprint
        return Outcome.Success(Unit)
    }

    override suspend fun download(): Outcome<String?> = failure ?: Outcome.Success(remoteJson)

    override suspend fun lastUploadedFingerprint(): String? = fingerprint

    override suspend fun disconnect() {
        state.value = CloudAccount.Disconnected
        fingerprint = null
    }

    companion object {
        const val EMAIL = "user@example.com"
    }
}
