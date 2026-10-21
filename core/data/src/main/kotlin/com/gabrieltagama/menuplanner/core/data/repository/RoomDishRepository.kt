package com.gabrieltagama.menuplanner.core.data.repository

import com.gabrieltagama.menuplanner.core.data.database.dao.DishDao
import com.gabrieltagama.menuplanner.core.data.database.entity.DishWithIngredients
import com.gabrieltagama.menuplanner.core.data.mapper.toDomain
import com.gabrieltagama.menuplanner.core.data.mapper.toEntity
import com.gabrieltagama.menuplanner.core.data.mapper.toIngredientEntities
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.PlannedUse
import com.gabrieltagama.menuplanner.core.domain.port.DishRepository
import com.gabrieltagama.menuplanner.core.domain.query.DishCatalogQueries
import com.gabrieltagama.menuplanner.core.domain.query.PlannedUseQueries
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed recipe book. Saving a dish replaces its ingredient lines atomically, saving several
 * dishes is one transaction and deleting a dish checks that it is not planned in the same transaction.
 * Planned uses are read from the lunch slots and dinners that reference the dish.
 */
internal class RoomDishRepository @Inject constructor(
    private val dishDao: DishDao
) : DishRepository, DishCatalogQueries, PlannedUseQueries {

    override fun observeDishes(): Flow<List<Dish>> = dishDao.observeAll().map { it.toDomainList() }

    override suspend fun getDishes(): List<Dish> = dishDao.getAll().toDomainList()

    override suspend fun getDish(id: DishId): Dish? = dishDao.getById(id.value)?.toDomain()

    override suspend fun upsert(dish: Dish) = dishDao.upsertWithIngredients(dish.toEntity(), dish.toIngredientEntities())

    override suspend fun upsertAll(dishes: List<Dish>) =
        dishDao.upsertAllWithIngredients(dishes.map { it.toEntity() to it.toIngredientEntities() })

    override suspend fun deleteIfUnused(id: DishId): Boolean = dishDao.deleteIfUnused(id.value)

    override suspend fun getPlannedUses(id: DishId): List<PlannedUse> =
        dishDao.getLunchUses(id.value).map { PlannedUse.Lunch(it.date, it.slot) } + dishDao.getDinnerDates(id.value).map(PlannedUse::Dinner)

    private fun List<DishWithIngredients>.toDomainList(): List<Dish> = map(DishWithIngredients::toDomain)
}
