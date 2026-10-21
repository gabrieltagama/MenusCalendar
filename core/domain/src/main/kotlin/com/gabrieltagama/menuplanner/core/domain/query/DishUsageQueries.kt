package com.gabrieltagama.menuplanner.core.domain.query

import com.gabrieltagama.menuplanner.core.domain.model.DishId
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Read port with the last day up to [until] (included) each dish was planned at lunch or dinner.
 * Implemented in :core:data.
 */
interface DishUsageQueries {
    fun observeLastServed(until: LocalDate): Flow<Map<DishId, LocalDate>>
}
