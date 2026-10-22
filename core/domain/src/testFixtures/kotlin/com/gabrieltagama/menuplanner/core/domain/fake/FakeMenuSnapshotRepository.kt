package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.model.share.MenuSnapshot
import com.gabrieltagama.menuplanner.core.domain.model.share.SnapshotScope
import com.gabrieltagama.menuplanner.core.domain.port.DishRepository
import com.gabrieltagama.menuplanner.core.domain.port.MenuSnapshotRepository

/**
 * In-memory MenuSnapshotRepository: read returns the dishes of the given repository and, for
 * EVERYTHING, the configured calendar entries.
 */
class FakeMenuSnapshotRepository(
    private val dishes: DishRepository,
    private val calendar: MenuSnapshot = MenuSnapshot(emptyList())
) : MenuSnapshotRepository {
    val readScopes = mutableListOf<SnapshotScope>()

    override suspend fun read(scope: SnapshotScope): MenuSnapshot {
        readScopes += scope
        val recipes = MenuSnapshot(dishes.getDishes())
        return if (scope == SnapshotScope.EVERYTHING) recipes.copy(lunches = calendar.lunches, dinners = calendar.dinners) else recipes
    }
}
