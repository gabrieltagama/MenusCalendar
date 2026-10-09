package com.gabrieltagama.menuplanner.core.data.fake

import com.gabrieltagama.menuplanner.core.data.database.entity.MealDayEntity
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import java.time.Instant
import java.time.LocalDate
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
        updatedAt: Instant = OLD_INSTANT
    ) = Dish(
        id = id,
        name = name,
        description = "Description of $name",
        ingredients = ingredients,
        preparation = "Prepare $name",
        type = type,
        heaviness = heaviness,
        updatedAt = updatedAt
    )

    fun singleDay(date: LocalDate, singleId: String, dessertId: String? = null, updatedAt: Instant = OLD_INSTANT) =
        MealDayEntity(
            date = date.toEpochDay(),
            starterId = null,
            mainId = null,
            singleId = singleId,
            dessertId = dessertId,
            updatedAt = updatedAt.toEpochMilli()
        )

    fun coursesDay(date: LocalDate, starterId: String, mainId: String, dessertId: String? = null, updatedAt: Instant = OLD_INSTANT) =
        MealDayEntity(
            date = date.toEpochDay(),
            starterId = starterId,
            mainId = mainId,
            singleId = null,
            dessertId = dessertId,
            updatedAt = updatedAt.toEpochMilli()
        )
}
