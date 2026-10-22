package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.port.MenuSnapshotCodec
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.StandardizeRecipeBookUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.share.ImportMenusUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.share.MenuSnapshotMerger

/**
 * Builds ImportMenusUseCase over the in-memory repositories, wired as Hilt does, for the tests of
 * imports, backups and the screens that use them.
 */
object TestImports {
    fun importMenus(
        dishes: FakeDishRepository,
        codec: MenuSnapshotCodec = FakeMenuSnapshotCodec(),
        lunches: FakeLunchPlanRepository = FakeLunchPlanRepository(),
        dinners: FakeDinnerPlanRepository = FakeDinnerPlanRepository()
    ): ImportMenusUseCase = ImportMenusUseCase(
        codec = codec,
        merger = MenuSnapshotMerger(dishes, lunches, dinners),
        standardizeRecipeBook = StandardizeRecipeBookUseCase(dishes),
        transaction = ImmediateTransactionRunner()
    )
}
