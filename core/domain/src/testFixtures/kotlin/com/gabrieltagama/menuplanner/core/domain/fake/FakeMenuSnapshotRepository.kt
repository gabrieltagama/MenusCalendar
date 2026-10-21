package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import com.gabrieltagama.menuplanner.core.domain.model.share.MenuSnapshot
import com.gabrieltagama.menuplanner.core.domain.model.share.SnapshotScope
import com.gabrieltagama.menuplanner.core.domain.port.DishRepository
import com.gabrieltagama.menuplanner.core.domain.port.MenuSnapshotRepository

/**
 * In-memory MenuSnapshotRepository: read returns the dishes of the given repository (and the
 * configured calendar entries for EVERYTHING), merge records the snapshot and returns a canned summary.
 */
class FakeMenuSnapshotRepository(
    private val dishes: DishRepository,
    private val calendar: MenuSnapshot = MenuSnapshot(emptyList()),
    private val mergeResult: ImportSummary = ImportSummary.EMPTY
) : MenuSnapshotRepository {
    val readScopes = mutableListOf<SnapshotScope>()
    val merged = mutableListOf<MenuSnapshot>()

    override suspend fun read(scope: SnapshotScope): MenuSnapshot {
        readScopes += scope
        val recipes = MenuSnapshot(dishes.getDishes())
        return if (scope == SnapshotScope.EVERYTHING) recipes.copy(lunches = calendar.lunches, dinners = calendar.dinners) else recipes
    }

    override suspend fun merge(snapshot: MenuSnapshot): ImportSummary {
        merged += snapshot
        return mergeResult
    }
}
