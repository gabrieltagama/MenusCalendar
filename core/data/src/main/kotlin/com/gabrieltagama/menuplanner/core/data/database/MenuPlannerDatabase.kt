package com.gabrieltagama.menuplanner.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gabrieltagama.menuplanner.core.data.database.dao.DinnerDayDao
import com.gabrieltagama.menuplanner.core.data.database.dao.DishDao
import com.gabrieltagama.menuplanner.core.data.database.dao.DishUsageDao
import com.gabrieltagama.menuplanner.core.data.database.dao.LunchDayDao
import com.gabrieltagama.menuplanner.core.data.database.dao.PlanQueryDao
import com.gabrieltagama.menuplanner.core.data.database.entity.DinnerDayEntity
import com.gabrieltagama.menuplanner.core.data.database.entity.DishEntity
import com.gabrieltagama.menuplanner.core.data.database.entity.IngredientEntity
import com.gabrieltagama.menuplanner.core.data.database.entity.LunchDayEntity

/**
 * Room database of the app. Every schema change bumps the version and adds a migration to
 * Migrations.ALL; destructive fallback is intentionally not used.
 */
@Database(
    entities = [DishEntity::class, IngredientEntity::class, LunchDayEntity::class, DinnerDayEntity::class],
    version = 4,
    exportSchema = true
)
internal abstract class MenuPlannerDatabase : RoomDatabase() {
    abstract fun dishDao(): DishDao
    abstract fun lunchDayDao(): LunchDayDao
    abstract fun dinnerDayDao(): DinnerDayDao
    abstract fun dishUsageDao(): DishUsageDao
    abstract fun planQueryDao(): PlanQueryDao

    companion object {
        const val NAME = "menuplanner.db"
    }
}
