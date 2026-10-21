package com.gabrieltagama.menuplanner.core.domain.port

import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import com.gabrieltagama.menuplanner.core.domain.model.share.MenuSnapshot
import com.gabrieltagama.menuplanner.core.domain.model.share.SnapshotScope

/**
 * Persistence port of the shared content. read takes a consistent snapshot of the recipe book and,
 * for EVERYTHING, of the calendar; merge applies a received snapshot in a single transaction with
 * DishMergePlan and ImportPolicy (a dish matches by id or, failing that, by DishKey), resolving
 * planned days against the dishes known after merging the incoming ones.
 * Implemented in :core:data.
 */
interface MenuSnapshotRepository {
    suspend fun read(scope: SnapshotScope): MenuSnapshot
    suspend fun merge(snapshot: MenuSnapshot): ImportSummary
}
