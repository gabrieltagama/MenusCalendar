package com.gabrieltagama.menuplanner.core.data.di

import android.content.Context
import androidx.room.Room
import com.gabrieltagama.menuplanner.core.data.database.MenuPlannerDatabase
import com.gabrieltagama.menuplanner.core.data.database.Migrations
import com.gabrieltagama.menuplanner.core.data.database.dao.DishDao
import com.gabrieltagama.menuplanner.core.data.database.dao.MealDayDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton
import kotlinx.serialization.json.Json

/**
 * Provides the Room database, its DAOs, the system clock and the JSON configuration used for sharing.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MenuPlannerDatabase =
        Room.databaseBuilder(context, MenuPlannerDatabase::class.java, MenuPlannerDatabase.NAME)
            .addMigrations(*Migrations.ALL)
            .build()

    @Provides
    fun provideDishDao(database: MenuPlannerDatabase): DishDao = database.dishDao()

    @Provides
    fun provideMealDayDao(database: MenuPlannerDatabase): MealDayDao = database.mealDayDao()

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}
