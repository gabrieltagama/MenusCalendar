package com.gabrieltagama.menuplanner.core.data.di

import com.gabrieltagama.menuplanner.core.data.initial.AssetInitialDishesRepository
import com.gabrieltagama.menuplanner.core.data.repository.RoomDinnerPlanRepository
import com.gabrieltagama.menuplanner.core.data.repository.RoomDishRepository
import com.gabrieltagama.menuplanner.core.data.repository.RoomDishUsageRepository
import com.gabrieltagama.menuplanner.core.data.repository.RoomMealPlanRepository
import com.gabrieltagama.menuplanner.core.data.repository.RoomMenuSnapshotRepository
import com.gabrieltagama.menuplanner.core.data.settings.DataStoreSettingsRepository
import com.gabrieltagama.menuplanner.core.data.share.JsonMenuSnapshotCodec
import com.gabrieltagama.menuplanner.core.data.shopping.DataStoreShoppingChecklistRepository
import com.gabrieltagama.menuplanner.core.domain.repository.DinnerPlanRepository
import com.gabrieltagama.menuplanner.core.domain.repository.DishIdGenerator
import com.gabrieltagama.menuplanner.core.domain.repository.DishRepository
import com.gabrieltagama.menuplanner.core.domain.repository.DishUsageRepository
import com.gabrieltagama.menuplanner.core.domain.repository.InitialDishesRepository
import com.gabrieltagama.menuplanner.core.domain.repository.MealPlanRepository
import com.gabrieltagama.menuplanner.core.domain.repository.MenuSnapshotCodec
import com.gabrieltagama.menuplanner.core.domain.repository.MenuSnapshotRepository
import com.gabrieltagama.menuplanner.core.domain.repository.SettingsRepository
import com.gabrieltagama.menuplanner.core.domain.repository.ShoppingChecklistRepository
import com.gabrieltagama.menuplanner.core.domain.repository.UuidDishIdGenerator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the domain repository ports to their data-layer implementations.
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
    abstract fun bindMenuSnapshotRepository(implementation: RoomMenuSnapshotRepository): MenuSnapshotRepository

    @Binds
    @Singleton
    abstract fun bindMenuSnapshotCodec(implementation: JsonMenuSnapshotCodec): MenuSnapshotCodec

    @Binds
    abstract fun bindDishIdGenerator(implementation: UuidDishIdGenerator): DishIdGenerator

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(implementation: DataStoreSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindInitialDishesRepository(implementation: AssetInitialDishesRepository): InitialDishesRepository

    @Binds
    @Singleton
    abstract fun bindDishUsageRepository(implementation: RoomDishUsageRepository): DishUsageRepository

    @Binds
    @Singleton
    abstract fun bindShoppingChecklistRepository(implementation: DataStoreShoppingChecklistRepository): ShoppingChecklistRepository
}
