package com.gabrieltagama.menuplanner.core.domain.query

import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.PlannedUse

/**
 * Read port with the calendar days where a dish is planned, in a lunch slot or as the dinner, so a
 * change of type or meal time can be checked against them. Implemented in :core:data.
 */
interface PlannedUseQueries {
    suspend fun getPlannedUses(id: DishId): List<PlannedUse>
}
