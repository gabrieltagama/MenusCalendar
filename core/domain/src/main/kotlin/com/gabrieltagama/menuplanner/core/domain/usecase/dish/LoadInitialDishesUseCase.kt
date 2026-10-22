package com.gabrieltagama.menuplanner.core.domain.usecase.dish

import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import com.gabrieltagama.menuplanner.core.domain.model.share.SnapshotScope
import com.gabrieltagama.menuplanner.core.domain.port.AppStateRepository
import com.gabrieltagama.menuplanner.core.domain.port.DishRepository
import com.gabrieltagama.menuplanner.core.domain.port.InitialDishesSource
import com.gabrieltagama.menuplanner.core.domain.usecase.share.ImportMenusUseCase
import javax.inject.Inject

/**
 * Loads the bundled starter recipe book once per installation, only while the recipe book is empty,
 * so users who already have dishes never receive it. It is imported like any shared file of recipes
 * (ImportMenusUseCase), so it is validated, merged and left in standard units the same way; its
 * dishes have fixed ids and an old updatedAt, so any later edit by the user wins. The result tells
 * what was imported, or the error of a broken bundled file.
 */
class LoadInitialDishesUseCase @Inject constructor(
    private val appState: AppStateRepository,
    private val dishes: DishRepository,
    private val source: InitialDishesSource,
    private val importMenus: ImportMenusUseCase
) {
    suspend operator fun invoke(): Outcome<ImportSummary> {
        if (appState.isInitialDishesLoaded()) return Outcome.Success(ImportSummary.EMPTY)
        val result = if (dishes.getDishes().isEmpty()) importMenus(source.read(), SnapshotScope.RECIPES) else Outcome.Success(ImportSummary.EMPTY)
        appState.setInitialDishesLoaded()
        return result
    }
}
