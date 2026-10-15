package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.repository.ShareFileStore

/**
 * In-memory ShareFileStore: written texts get a numbered location, files can be preset by
 * location, and failing makes every write and read return null.
 */
class FakeShareFileStore(var failing: Boolean = false) : ShareFileStore {
    val files = mutableMapOf<String, String>()

    override suspend fun write(json: String): String? {
        if (failing) return null
        val location = "file-${files.size + 1}"
        files[location] = json
        return location
    }

    override suspend fun read(location: String): String? = if (failing) null else files[location]
}
