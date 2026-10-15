package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.port.InitialDishesSource

/**
 * In-memory InitialDishesSource with a canned JSON and the loaded flag, counting reads.
 */
class FakeInitialDishesSource(private val json: String = "starter", var loaded: Boolean = false) : InitialDishesSource {
    var reads = 0
        private set

    override suspend fun isLoaded(): Boolean = loaded

    override suspend fun read(): String {
        reads++
        return json
    }

    override suspend fun markLoaded() {
        loaded = true
    }
}
