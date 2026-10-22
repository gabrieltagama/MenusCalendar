package com.gabrieltagama.menuplanner.core.domain.port

/**
 * Port that reads the starter recipe book bundled with the app, as a shared file of recipes only.
 * Implemented in :core:data from the app assets.
 */
fun interface InitialDishesSource {
    suspend fun read(): String
}
