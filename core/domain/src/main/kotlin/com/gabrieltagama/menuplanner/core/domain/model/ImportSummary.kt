package com.gabrieltagama.menuplanner.core.domain.model

/**
 * Result of merging a shared JSON into the local database.
 */
data class ImportSummary(
    val dishesAdded: Int,
    val dishesUpdated: Int,
    val dishesSkipped: Int,
    val daysAdded: Int,
    val daysUpdated: Int,
    val daysSkipped: Int
)
