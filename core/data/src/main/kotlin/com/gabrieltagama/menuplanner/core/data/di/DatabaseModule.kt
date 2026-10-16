package com.gabrieltagama.menuplanner.core.data.di

import android.content.Context
import androidx.room.Room
import com.gabrieltagama.menuplanner.core.data.database.MenuPlannerDatabase
import com.gabrieltagama.menuplanner.core.data.database.Migrations
import com.gabrieltagama.menuplanner.core.data.database.dao.DinnerDayDao
import com.gabrieltagama.menuplanner.core.data.database.dao.DishDao
import com.gabrieltagama.menuplanner.core.data.database.dao.DishUsageDao
import com.gabrieltagama.menuplanner.core.data.database.dao.MealDayDao
import com.gabrieltagama.menuplanner.core.data.database.dao.PlanQueryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the Room database and its DAOs.
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
    fun provideDinnerDayDao(database: MenuPlannerDatabase): DinnerDayDao = database.dinnerDayDao()

    @Provides
    fun provideDishUsageDao(database: MenuPlannerDatabase): DishUsageDao = database.dishUsageDao()

    @Provides
    fun providePlanQueryDao(database: MenuPlannerDatabase): PlanQueryDao = database.planQueryDao()
}
