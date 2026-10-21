package com.gabrieltagama.menuplanner.core.cloud.drive

/**
 * Operations of the backup on the hidden app data folder of Google Drive. An HTTP 401 surfaces as
 * DriveUnauthorizedException and any other failure as IOException. DriveAppDataClient is the
 * production implementation.
 */
internal interface DriveFiles {
    suspend fun accountEmail(token: String): String
    suspend fun findBackups(token: String): List<DriveFile>
    suspend fun download(token: String, fileId: String): String
    suspend fun create(token: String, content: String)
    suspend fun delete(token: String, fileId: String)
}
