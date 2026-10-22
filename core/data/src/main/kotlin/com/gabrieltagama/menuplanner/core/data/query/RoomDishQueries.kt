package com.gabrieltagama.menuplanner.core.data.query

import com.gabrieltagama.menuplanner.core.data.database.dao.DishDao
import com.gabrieltagama.menuplanner.core.data.database.entity.DishListRow
import com.gabrieltagama.menuplanner.core.data.database.entity.DishWithIngredients
import com.gabrieltagama.menuplanner.core.data.mapper.toDomain
import com.gabrieltagama.menuplanner.core.data.mapper.toListItem
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.DishListItem
import com.gabrieltagama.menuplanner.core.domain.model.PlannedUse
import com.gabrieltagama.menuplanner.core.domain.query.DishCatalogQueries
import com.gabrieltagama.menuplanner.core.domain.query.PlannedUseQueries
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed read side of the recipe book: the list rows (DishListItem) and the whole dishes for the
 * screens, and the calendar days where a dish is planned, read from the lunch slots and dinners that
 * reference it. The writes live in RoomDishRepository.
 */
internal class RoomDishQueries @Inject constructor(private val dishDao: DishDao) : DishCatalogQueries, PlannedUseQueries {

    override fun observeDishList(): Flow<List<DishListItem>> = dishDao.observeList().map { rows -> rows.map(DishListRow::toListItem) }

    override fun observeDishes(): Flow<List<Dish>> = dishDao.observeAll().map { rows -> rows.map(DishWithIngredients::toDomain) }

    override suspend fun getPlannedUses(id: DishId): List<PlannedUse> =
        dishDao.getLunchUses(id.value).map { PlannedUse.Lunch(it.date, it.slot) } + dishDao.getDinnerDates(id.value).map(PlannedUse::Dinner)
}
