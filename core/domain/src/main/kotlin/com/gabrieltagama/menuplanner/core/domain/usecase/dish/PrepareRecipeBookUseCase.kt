package com.gabrieltagama.menuplanner.core.domain.usecase.dish

import javax.inject.Inject

/**
 * Start-up preparation of the recipe book: loads the bundled starter dishes on the first launch
 * after installing the app and then leaves every dish in standard units, which also updates data
 * stored by older versions.
 */
class PrepareRecipeBookUseCase @Inject constructor(
    private val loadInitialDishes: LoadInitialDishesUseCase,
    private val standardizeRecipeBook: StandardizeRecipeBookUseCase
) {
    suspend operator fun invoke() {
        loadInitialDishes()
        standardizeRecipeBook()
    }
}
