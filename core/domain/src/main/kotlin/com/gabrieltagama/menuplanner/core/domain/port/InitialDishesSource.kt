package com.gabrieltagama.menuplanner.core.domain.port

/**
 * Port to the starter recipe book bundled with the app: its share JSON and a per-installation flag
 * telling whether it was already offered. Implemented in :core:data.
 */
interface InitialDishesSource {
    suspend fun isLoaded(): Boolean
    suspend fun read(): String
    suspend fun markLoaded()
}
