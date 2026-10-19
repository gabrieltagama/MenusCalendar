package com.gabrieltagama.menuplanner.core.data.database.entity

/**
 * Projection of a lunch where a dish is planned: the epoch day and the name of the slot it fills.
 */
internal data class LunchUse(
    val date: Long,
    val slot: String
)
