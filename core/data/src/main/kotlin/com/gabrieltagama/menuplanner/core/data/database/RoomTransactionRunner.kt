package com.gabrieltagama.menuplanner.core.data.database

import androidx.room.withTransaction
import com.gabrieltagama.menuplanner.core.domain.repository.TransactionRunner
import javax.inject.Inject

/**
 * TransactionRunner backed by a Room transaction on the app database; nested calls join the outer
 * transaction.
 */
internal class RoomTransactionRunner @Inject constructor(private val database: MenuPlannerDatabase) : TransactionRunner {

    override suspend fun <R> run(block: suspend () -> R): R = database.withTransaction(block)
}
