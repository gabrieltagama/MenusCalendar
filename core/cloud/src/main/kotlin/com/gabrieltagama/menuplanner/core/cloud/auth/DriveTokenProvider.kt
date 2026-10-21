package com.gabrieltagama.menuplanner.core.cloud.auth

/**
 * What the backup needs from the Google authorization: a fresh access token for the linked account
 * without showing any UI (null when the user has to grant access again), and dropping a rejected
 * token or the whole grant. GoogleDriveAuthorizer is the production implementation.
 */
interface DriveTokenProvider {
    suspend fun silentToken(email: String): String?
    suspend fun invalidate(token: String)
    suspend fun revoke(email: String)
}
