package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import com.gabrieltagama.menuplanner.core.domain.repository.MenuShareRepository

/**
 * In-memory MenuShareRepository returning canned results and recording imported JSON.
 */
class FakeMenuShareRepository(
    private val exportJson: String = "{}",
    private val recipesJson: String = "{}",
    private val importResult: Outcome<ImportSummary> = Outcome.Success(ImportSummary(0, 0, 0, 0, 0, 0))
) : MenuShareRepository {
    val importedJson = mutableListOf<String>()
    var exportCalls = 0
        private set

    override suspend fun exportAll(): String {
        exportCalls++
        return exportJson
    }

    override suspend fun exportRecipes(): String = recipesJson

    override suspend fun importAndMerge(json: String): Outcome<ImportSummary> {
        importedJson += json
        return importResult
    }
}
