package com.gabrieltagama.menuplanner.core.data.di

import com.gabrieltagama.menuplanner.core.data.database.RoomTransactionRunner
import com.gabrieltagama.menuplanner.core.data.id.UuidDishIdGenerator
import com.gabrieltagama.menuplanner.core.data.initial.AssetInitialDishesRepository
import com.gabrieltagama.menuplanner.core.data.query.RoomDishUsageQueries
import com.gabrieltagama.menuplanner.core.data.repository.RoomDinnerPlanRepository
import com.gabrieltagama.menuplanner.core.data.repository.RoomDishRepository
import com.gabrieltagama.menuplanner.core.data.repository.RoomLunchPlanRepository
import com.gabrieltagama.menuplanner.core.data.repository.RoomMenuSnapshotRepository
import com.gabrieltagama.menuplanner.core.data.settings.DataStoreSettingsRepository
import com.gabrieltagama.menuplanner.core.data.share.JsonMenuSnapshotCodec
import com.gabrieltagama.menuplanner.core.data.shopping.DataStoreShoppingChecklistRepository
import com.gabrieltagama.menuplanner.core.domain.port.DinnerPlanRepository
import com.gabrieltagama.menuplanner.core.domain.port.DishIdGenerator
import com.gabrieltagama.menuplanner.core.domain.port.DishRepository
import com.gabrieltagama.menuplanner.core.domain.port.InitialDishesRepository
import com.gabrieltagama.menuplanner.core.domain.port.LunchPlanRepository
import com.gabrieltagama.menuplanner.core.domain.port.MenuSnapshotCodec
import com.gabrieltagama.menuplanner.core.domain.port.MenuSnapshotRepository
import com.gabrieltagama.menuplanner.core.domain.port.SettingsRepository
import com.gabrieltagama.menuplanner.core.domain.port.ShoppingChecklistRepository
import com.gabrieltagama.menuplanner.core.domain.port.TransactionRunner
import com.gabrieltagama.menuplanner.core.domain.query.DishUsageQueries
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
    abstract fun bindLunchPlanRepository(implementation: RoomLunchPlanRepository): LunchPlanRepository

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
    abstract fun bindTransactionRunner(implementation: RoomTransactionRunner): TransactionRunner

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(implementation: DataStoreSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindInitialDishesRepository(implementation: AssetInitialDishesRepository): InitialDishesRepository

    @Binds
    @Singleton
    abstract fun bindDishUsageQueries(implementation: RoomDishUsageQueries): DishUsageQueries

    @Binds
    @Singleton
    abstract fun bindShoppingChecklistRepository(implementation: DataStoreShoppingChecklistRepository): ShoppingChecklistRepository
}
