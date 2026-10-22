package com.gabrieltagama.menuplanner.core.domain.query

import com.gabrieltagama.menuplanner.core.domain.model.CloudAccount
import kotlinx.coroutines.flow.Flow

/**
 * Read port of the Google account linked to the recipe backup. CloudBackupRepository adds the
 * backup operations. Implemented in :core:cloud.
 */
interface CloudAccountQueries {
    val account: Flow<CloudAccount>
}
