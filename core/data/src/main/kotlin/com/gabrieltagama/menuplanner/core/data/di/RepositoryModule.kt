package com.gabrieltagama.menuplanner.core.data.di

import com.gabrieltagama.menuplanner.core.data.auth.LocalPinAuthRepository
import com.gabrieltagama.menuplanner.core.data.repository.JsonMenuShareRepository
import com.gabrieltagama.menuplanner.core.data.repository.RoomDishRepository
import com.gabrieltagama.menuplanner.core.data.repository.RoomMealPlanRepository
import com.gabrieltagama.menuplanner.core.data.settings.SharedPreferencesSettingsRepository
import com.gabrieltagama.menuplanner.core.domain.repository.AuthRepository
import com.gabrieltagama.menuplanner.core.domain.repository.DishRepository
import com.gabrieltagama.menuplanner.core.domain.repository.MealPlanRepository
import com.gabrieltagama.menuplanner.core.domain.repository.MenuShareRepository
import com.gabrieltagama.menuplanner.core.domain.repository.SettingsRepository
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
    abstract fun bindMenuShareRepository(implementation: JsonMenuShareRepository): MenuShareRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(implementation: LocalPinAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(implementation: SharedPreferencesSettingsRepository): SettingsRepository
}
