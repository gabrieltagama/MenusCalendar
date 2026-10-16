package com.gabrieltagama.menuplanner

import com.gabrieltagama.menuplanner.core.domain.usecase.dish.LoadInitialDishesUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.MigrateRecipeUnitsUseCase
import com.gabrieltagama.menuplanner.di.ApplicationScope
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Work done once per process start, outside any screen: load the bundled starter recipe book on the
 * first launch after installing and convert the recipe book to standard units when the conversion
 * version changed.
 */
class AppStartup @Inject constructor(
    private val loadInitialDishes: LoadInitialDishesUseCase,
    private val migrateRecipeUnits: MigrateRecipeUnitsUseCase,
    @param:ApplicationScope private val scope: CoroutineScope
) {
    fun start() {
        scope.launch {
            loadInitialDishes()
            migrateRecipeUnits()
        }
    }
}
