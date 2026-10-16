package com.gabrieltagama.menuplanner.core.domain.repository

import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.share.MenuSnapshot
import java.time.Instant

/**
 * Port of the file format used to share and back up a snapshot. decode rejects files that are
 * malformed, newer than supported or that contain dishes breaking the Dish rules. Implemented in
 * :core:data as JSON.
 */
interface MenuSnapshotCodec {
    fun encode(snapshot: MenuSnapshot, exportedAt: Instant): String
    fun decode(text: String): Outcome<MenuSnapshot>
}
