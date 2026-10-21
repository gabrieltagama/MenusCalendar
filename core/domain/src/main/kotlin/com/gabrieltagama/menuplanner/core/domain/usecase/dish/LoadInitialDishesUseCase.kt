package com.gabrieltagama.menuplanner.core.domain.usecase.dish

import com.gabrieltagama.menuplanner.core.domain.port.InitialDishesRepository
import javax.inject.Inject

/**
 * Loads the bundled starter recipe book on the first launch after installing the app.
 */
class LoadInitialDishesUseCase @Inject constructor(private val repository: InitialDishesRepository) {
    suspend operator fun invoke() = repository.loadIfFirstLaunch()
}
