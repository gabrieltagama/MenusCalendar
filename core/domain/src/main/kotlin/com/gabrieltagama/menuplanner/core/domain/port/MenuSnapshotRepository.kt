package com.gabrieltagama.menuplanner.core.domain.port

import com.gabrieltagama.menuplanner.core.domain.model.share.MenuSnapshot
import com.gabrieltagama.menuplanner.core.domain.model.share.SnapshotScope

/**
 * Persistence port that takes a consistent snapshot of the recipe book and, for EVERYTHING, of the
 * calendar, to share or back it up. A received snapshot is merged by MenuSnapshotMerger through the
 * recipe book and calendar repositories. Implemented in :core:data.
 */
interface MenuSnapshotRepository {
    suspend fun read(scope: SnapshotScope): MenuSnapshot
}
