package com.gabrieltagama.menuplanner.core.domain.repository

import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary

/**
 * Port for sharing data as JSON. exportAll returns the JSON text of every dish and planned day,
 * exportRecipes only the dishes (used by the cloud backup); importAndMerge merges a received JSON: unknown items are added, known items are replaced only
 * when the incoming updatedAt is newer.
 */
interface MenuShareRepository {
    suspend fun exportAll(): String
    suspend fun exportRecipes(): String
    suspend fun importAndMerge(json: String): Outcome<ImportSummary>
}
