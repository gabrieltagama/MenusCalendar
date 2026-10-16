package com.gabrieltagama.menuplanner.core.data.fake

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.model.Rating
import java.time.Instant
import kotlinx.serialization.json.Json

/**
 * Test-data builders for core:data JVM tests. Instants use millisecond precision so they survive
 * the epoch-millis storage used by Room.
 */
internal object TestData {
    val OLD_INSTANT: Instant = Instant.parse("2025-01-01T08:00:00.123Z")
    val NEW_INSTANT: Instant = Instant.parse("2026-03-10T12:30:45.678Z")
    val EXPORTED_AT: Instant = Instant.parse("2026-05-01T00:00:00Z")

    val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun dish(
        id: String,
        name: String = "Dish $id",
        type: DishType = DishType.MAIN,
        heaviness: Heaviness = Heaviness.MEDIUM,
        ingredients: List<Ingredient> = emptyList(),
        rating: Rating = Rating.THREE,
        mealTime: MealTime = MealTime.ANY,
        updatedAt: Instant = OLD_INSTANT
    ) = Dish(
        id = DishId(id),
        name = name,
        description = "Description of $name",
        ingredients = ingredients,
        preparation = "Prepare $name",
        type = type,
        heaviness = heaviness,
        rating = rating,
        mealTime = mealTime,
        updatedAt = updatedAt
    )
}
