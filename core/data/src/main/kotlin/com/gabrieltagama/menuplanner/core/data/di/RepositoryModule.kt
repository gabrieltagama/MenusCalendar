package com.gabrieltagama.menuplanner.core.data.di

import com.gabrieltagama.menuplanner.core.data.database.RoomTransactionRunner
import com.gabrieltagama.menuplanner.core.data.initial.AssetInitialDishesSource
import com.gabrieltagama.menuplanner.core.data.repository.JsonMenuShareRepository
import com.gabrieltagama.menuplanner.core.data.repository.RoomDinnerPlanRepository
import com.gabrieltagama.menuplanner.core.data.repository.RoomDishRepository
import com.gabrieltagama.menuplanner.core.data.repository.RoomMealPlanRepository
import com.gabrieltagama.menuplanner.core.data.settings.SharedPreferencesSettingsRepository
import com.gabrieltagama.menuplanner.core.domain.repository.DinnerPlanRepository
import com.gabrieltagama.menuplanner.core.domain.repository.DishRepository
import com.gabrieltagama.menuplanner.core.domain.repository.InitialDishesSource
import com.gabrieltagama.menuplanner.core.domain.repository.MealPlanRepository
import com.gabrieltagama.menuplanner.core.domain.repository.MenuShareRepository
import com.gabrieltagama.menuplanner.core.domain.repository.SettingsRepository
import com.gabrieltagama.menuplanner.core.domain.repository.TransactionRunner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the domain ports (repositories, the initial recipe book source and the transaction
 * runner) to their data-layer implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDishRepository(implementation: RoomDishRepository): DishRepository

    @Binds
    @Singleton
    abstract fun bindMealPlanRepository(implementation: RoomMealPlanRepository): MealPlanRepository

    @Binds
    @Singleton
    abstract fun bindDinnerPlanRepository(implementation: RoomDinnerPlanRepository): DinnerPlanRepository

    @Binds
    @Singleton
    abstract fun bindMenuShareRepository(implementation: JsonMenuShareRepository): MenuShareRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(implementation: SharedPreferencesSettingsRepository): SettingsRepository

    @Binds
    abstract fun bindInitialDishesSource(implementation: AssetInitialDishesSource): InitialDishesSource

    @Binds
    abstract fun bindTransactionRunner(implementation: RoomTransactionRunner): TransactionRunner
}
