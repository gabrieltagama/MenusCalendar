package com.gabrieltagama.menuplanner.core.data.repository

import androidx.room.withTransaction
import com.gabrieltagama.menuplanner.core.data.database.MenuPlannerDatabase
import com.gabrieltagama.menuplanner.core.data.database.entity.DishWithIngredients
import com.gabrieltagama.menuplanner.core.data.mapper.toDomain
import com.gabrieltagama.menuplanner.core.data.mapper.toEntry
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.share.MenuSnapshot
import com.gabrieltagama.menuplanner.core.domain.model.share.SnapshotScope
import com.gabrieltagama.menuplanner.core.domain.port.MenuSnapshotRepository
import javax.inject.Inject

/**
 * Room-backed snapshot of the recipe book and, for EVERYTHING, of the calendar, read in one
 * consistent transaction. Planned days keep their dish ids as stored (toEntry).
 */
internal class RoomMenuSnapshotRepository @Inject constructor(
    private val database: MenuPlannerDatabase
) : MenuSnapshotRepository {

    private val dishDao get() = database.dishDao()
    private val lunchDayDao get() = database.lunchDayDao()
    private val dinnerDayDao get() = database.dinnerDayDao()

    override suspend fun read(scope: SnapshotScope): MenuSnapshot = database.withTransaction {
        MenuSnapshot(
            dishes = allDishes(),
            lunches = if (scope == SnapshotScope.EVERYTHING) lunchDayDao.getAll().map { it.toEntry() } else emptyList(),
            dinners = if (scope == SnapshotScope.EVERYTHING) dinnerDayDao.getAll().map { it.toEntry() } else emptyList()
        )
    }

    private suspend fun allDishes(): List<Dish> = dishDao.getAll().map(DishWithIngredients::toDomain)
}
