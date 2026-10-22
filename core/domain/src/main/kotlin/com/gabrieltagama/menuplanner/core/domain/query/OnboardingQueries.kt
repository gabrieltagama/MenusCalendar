package com.gabrieltagama.menuplanner.core.domain.query

import kotlinx.coroutines.flow.Flow

/**
 * Read port that tells whether the welcome screen was already answered. AppStateRepository adds
 * the changes. Implemented in :core:data.
 */
interface OnboardingQueries {
    val isOnboardingCompleted: Flow<Boolean>
}
