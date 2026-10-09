package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

/**
 * Test-data builders and a fixed clock shared by domain tests.
 */
object TestDishes {
    val FIXED_INSTANT: Instant = Instant.parse("2026-01-15T10:00:00Z")
    val OLD_INSTANT: Instant = Instant.parse("2020-01-01T00:00:00Z")
    val fixedClock: Clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC)

    fun dish(
        id: String = UUID.randomUUID().toString(),
        name: String = "Dish",
        type: DishType = DishType.MAIN,
        heaviness: Heaviness = Heaviness.MEDIUM,
        ingredients: List<Ingredient> = emptyList(),
        description: String = "",
        preparation: String = "",
        updatedAt: Instant = OLD_INSTANT
    ) = Dish(
        id = id,
        name = name,
        description = description,
        ingredients = ingredients,
        preparation = preparation,
        type = type,
        heaviness = heaviness,
        updatedAt = updatedAt
    )
}
