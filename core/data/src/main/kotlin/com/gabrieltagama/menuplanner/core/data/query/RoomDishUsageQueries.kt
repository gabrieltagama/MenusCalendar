package com.gabrieltagama.menuplanner.core.data.query

import com.gabrieltagama.menuplanner.core.data.database.dao.DishUsageDao
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.query.DishUsageQueries
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Room-backed last served day of each dish, refreshed whenever either calendar changes.
 */
internal class RoomDishUsageQueries @Inject constructor(private val dao: DishUsageDao) : DishUsageQueries {

    override fun observeLastServed(until: LocalDate): Flow<Map<DishId, LocalDate>> =
        dao.observeLastServed(until.toEpochDay())
            .map { rows -> rows.associate { DishId(it.dishId) to LocalDate.ofEpochDay(it.lastDate) } }
            .distinctUntilChanged()
}
