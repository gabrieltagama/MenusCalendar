package com.gabrieltagama.menuplanner.core.domain.usecase.dish

import com.gabrieltagama.menuplanner.core.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Converts the stored recipe book to standard units once per conversion version, on app start,
 * instead of reading and rewriting every dish on each launch. Bump CURRENT_VERSION whenever the
 * conversion rules or the ingredient catalog change, so existing books are converted again.
 */
class MigrateRecipeUnitsUseCase @Inject constructor(
    private val settings: SettingsRepository,
    private val standardizeRecipeBook: StandardizeRecipeBookUseCase
) {
    suspend operator fun invoke() {
        if (settings.recipeUnitsVersion() >= CURRENT_VERSION) return
        standardizeRecipeBook()
        settings.setRecipeUnitsVersion(CURRENT_VERSION)
    }

    companion object {
        const val CURRENT_VERSION = 1
    }
}
