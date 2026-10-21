package com.gabrieltagama.menuplanner.core.domain.port

import kotlinx.coroutines.flow.Flow

/**
 * Persistence port for the state the app keeps for itself: whether the welcome screen was already
 * answered and the version of the unit conversion already applied to the recipe book (see
 * MigrateRecipeUnitsUseCase). Implemented in :core:data.
 */
interface AppStateRepository {
    val isOnboardingCompleted: Flow<Boolean>
    suspend fun completeOnboarding()
    suspend fun recipeUnitsVersion(): Int
    suspend fun setRecipeUnitsVersion(version: Int)
}
