package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.model.Rating
import com.gabrieltagama.menuplanner.core.domain.repository.DishIdGenerator
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
    val GENERATED_ID = DishId("generated-id")
    val idGenerator = DishIdGenerator { GENERATED_ID }

    fun dish(
        id: String = UUID.randomUUID().toString(),
        name: String = "Dish",
        type: DishType = DishType.MAIN,
        heaviness: Heaviness = Heaviness.MEDIUM,
        ingredients: List<Ingredient> = emptyList(),
        description: String = "",
        preparation: String = "",
        rating: Rating = Rating.THREE,
        mealTime: MealTime = MealTime.ANY,
        updatedAt: Instant = OLD_INSTANT
    ) = Dish(
        id = DishId(id),
        name = name,
        description = description,
        ingredients = ingredients,
        preparation = preparation,
        type = type,
        heaviness = heaviness,
        rating = rating,
        mealTime = mealTime,
        updatedAt = updatedAt
    )
}
