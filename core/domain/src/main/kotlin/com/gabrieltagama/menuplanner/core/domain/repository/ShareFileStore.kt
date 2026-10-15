package com.gabrieltagama.menuplanner.core.domain.repository

/**
 * Port to the files exchanged when sharing. write stores an export and returns a location other
 * apps can open (a content URI as text), or null when it could not be written; read returns the
 * text at a location the user picked, or null when it cannot be read. Implemented in :core:data.
 */
interface ShareFileStore {
    suspend fun write(json: String): String?
    suspend fun read(location: String): String?
}
