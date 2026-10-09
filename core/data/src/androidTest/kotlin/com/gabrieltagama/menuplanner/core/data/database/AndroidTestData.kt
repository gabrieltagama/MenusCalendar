package com.gabrieltagama.menuplanner.core.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import java.time.Instant
import java.time.LocalDate

/**
 * Shared builders for instrumented tests: an in-memory database and domain objects whose instants
 * have millisecond precision so they compare equal after the epoch-millis round trip.
 */
internal object AndroidTestData {
    val OLD_INSTANT: Instant = Instant.parse("2025-01-01T08:00:00.123Z")
    val NEW_INSTANT: Instant = Instant.parse("2026-03-10T12:30:45.678Z")

    fun inMemoryDatabase(): MenuPlannerDatabase =
        Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), MenuPlannerDatabase::class.java)
            .build()

    fun dish(
        id: String,
        name: String = "Dish $id",
        type: DishType = DishType.MAIN,
        ingredients: List<Ingredient> = emptyList(),
        updatedAt: Instant = OLD_INSTANT
    ) = Dish(
        id = id,
        name = name,
        description = "Description of $name",
        ingredients = ingredients,
        preparation = "Prepare $name",
        type = type,
        heaviness = Heaviness.MEDIUM,
        updatedAt = updatedAt
    )

    fun singleDay(date: LocalDate, single: Dish, dessert: Dish? = null, updatedAt: Instant = OLD_INSTANT) =
        MealDay(date = date, menu = DailyMenu.Single(single = single, dessert = dessert), updatedAt = updatedAt)

    fun coursesDay(date: LocalDate, starter: Dish, main: Dish, dessert: Dish? = null, updatedAt: Instant = OLD_INSTANT) =
        MealDay(date = date, menu = DailyMenu.Courses(starter = starter, main = main, dessert = dessert), updatedAt = updatedAt)
}
