package com.gabrieltagama.menuplanner.core.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * The dinner planned for one calendar day: always a single dish (see Dish.isDinnerOption).
 */
data class DinnerDay(
    val date: LocalDate,
    val dish: Dish,
    val updatedAt: Instant = Instant.now()
)
