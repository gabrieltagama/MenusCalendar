package com.gabrieltagama.menuplanner.core.domain.repository

import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.ImportContent

/**
 * Port for the JSON share format. exportAll returns the JSON text of every dish and planned day,
 * exportRecipes only the dishes (used by the cloud backup); decode validates a received JSON and
 * returns its content without touching the stored data, which ImportMenusUseCase then merges.
 */
interface MenuShareRepository {
    suspend fun exportAll(): String
    suspend fun exportRecipes(): String
    suspend fun decode(json: String): Outcome<ImportContent>
}
