package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.ImportContent
import com.gabrieltagama.menuplanner.core.domain.port.MenuShareRepository

/**
 * In-memory MenuShareRepository returning canned export texts and a canned decode result, and
 * recording every decoded JSON.
 */
class FakeMenuShareRepository(
    private val exportJson: String = "{}",
    private val recipesJson: String = "{}",
    var decodeResult: Outcome<ImportContent> = Outcome.Success(ImportContent(emptyList()))
) : MenuShareRepository {
    val decodedJson = mutableListOf<String>()
    var exportCalls = 0
        private set

    override suspend fun exportAll(): String {
        exportCalls++
        return exportJson
    }

    override suspend fun exportRecipes(): String = recipesJson

    override suspend fun decode(json: String): Outcome<ImportContent> {
        decodedJson += json
        return decodeResult
    }
}
