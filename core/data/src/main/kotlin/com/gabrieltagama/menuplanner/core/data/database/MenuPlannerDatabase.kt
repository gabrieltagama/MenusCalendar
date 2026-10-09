package com.gabrieltagama.menuplanner.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gabrieltagama.menuplanner.core.data.database.dao.DishDao
import com.gabrieltagama.menuplanner.core.data.database.dao.MealDayDao
import com.gabrieltagama.menuplanner.core.data.database.entity.DishEntity
import com.gabrieltagama.menuplanner.core.data.database.entity.IngredientEntity
import com.gabrieltagama.menuplanner.core.data.database.entity.MealDayEntity

/**
 * Room database of the app. Every schema change bumps the version and adds a migration to
 * Migrations.ALL; destructive fallback is intentionally not used.
 */
@Database(
    entities = [DishEntity::class, IngredientEntity::class, MealDayEntity::class],
    version = 1,
    exportSchema = true
)
internal abstract class MenuPlannerDatabase : RoomDatabase() {
    abstract fun dishDao(): DishDao
    abstract fun mealDayDao(): MealDayDao

    companion object {
        const val NAME = "menuplanner.db"
    }
}
