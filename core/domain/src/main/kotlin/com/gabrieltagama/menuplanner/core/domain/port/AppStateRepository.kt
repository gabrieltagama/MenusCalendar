package com.gabrieltagama.menuplanner.core.domain.port

import com.gabrieltagama.menuplanner.core.domain.query.OnboardingQueries

/**
 * Persistence port for the state the app keeps for itself: marking the welcome screen as answered
 * (read through OnboardingQueries) and the version of the unit conversion already applied to the
 * recipe book (see MigrateRecipeUnitsUseCase). Implemented in :core:data.
 */
interface AppStateRepository : OnboardingQueries {
    suspend fun completeOnboarding()
    suspend fun recipeUnitsVersion(): Int
    suspend fun setRecipeUnitsVersion(version: Int)
}
