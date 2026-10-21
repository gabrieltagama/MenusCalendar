package com.gabrieltagama.menuplanner.core.data.database

import androidx.room.withTransaction
import com.gabrieltagama.menuplanner.core.domain.port.TransactionRunner
import javax.inject.Inject

/**
 * TransactionRunner backed by a Room transaction; repository calls inside the block join it, so
 * their writes are committed together.
 */
internal class RoomTransactionRunner @Inject constructor(private val database: MenuPlannerDatabase) : TransactionRunner {
    override suspend fun <T> inTransaction(block: suspend () -> T): T = database.withTransaction(block)
}
