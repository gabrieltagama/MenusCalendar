package com.gabrieltagama.menuplanner.core.data.database.entity

import com.gabrieltagama.menuplanner.core.domain.model.MenuSlot
import java.time.LocalDate

/**
 * Projection of a lunch where a dish is planned: the epoch day and the name of the slot it fills.
 */
internal data class LunchUse(
    val date: LocalDate,
    val slot: MenuSlot
)
