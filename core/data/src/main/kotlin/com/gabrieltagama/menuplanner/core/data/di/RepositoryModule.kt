package com.gabrieltagama.menuplanner.core.data.di

import com.gabrieltagama.menuplanner.core.data.database.RoomTransactionRunner
import com.gabrieltagama.menuplanner.core.data.initial.AssetInitialDishesSource
import com.gabrieltagama.menuplanner.core.data.repository.JsonMenuShareRepository
import com.gabrieltagama.menuplanner.core.data.repository.RoomDinnerPlanRepository
import com.gabrieltagama.menuplanner.core.data.repository.RoomDishRepository
import com.gabrieltagama.menuplanner.core.data.repository.RoomMealPlanRepository
import com.gabrieltagama.menuplanner.core.data.settings.SharedPreferencesSettingsRepository
import com.gabrieltagama.menuplanner.core.data.share.CacheShareFileStore
import com.gabrieltagama.menuplanner.core.domain.port.DinnerPlanRepository
import com.gabrieltagama.menuplanner.core.domain.port.DishRepository
import com.gabrieltagama.menuplanner.core.domain.port.InitialDishesSource
import com.gabrieltagama.menuplanner.core.domain.port.MealPlanRepository
import com.gabrieltagama.menuplanner.core.domain.port.MenuShareRepository
import com.gabrieltagama.menuplanner.core.domain.port.SettingsRepository
import com.gabrieltagama.menuplanner.core.domain.port.ShareFileStore
import com.gabrieltagama.menuplanner.core.domain.port.TransactionRunner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the domain ports (repositories, the initial recipe book source, the transaction runner
 * and the share file store) to their data-layer implementations.
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

    @Binds
    abstract fun bindShareFileStore(implementation: CacheShareFileStore): ShareFileStore
}
