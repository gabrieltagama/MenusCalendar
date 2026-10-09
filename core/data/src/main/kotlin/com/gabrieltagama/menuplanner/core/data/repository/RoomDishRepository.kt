package com.gabrieltagama.menuplanner.core.data.repository

import com.gabrieltagama.menuplanner.core.data.database.dao.DishDao
import com.gabrieltagama.menuplanner.core.data.database.entity.DishWithIngredients
import com.gabrieltagama.menuplanner.core.data.mapper.toDomain
import com.gabrieltagama.menuplanner.core.data.mapper.toEntity
import com.gabrieltagama.menuplanner.core.data.mapper.toIngredientEntities
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.repository.DishRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed recipe book. Saving a dish replaces its ingredient lines atomically.
 */
internal class RoomDishRepository @Inject constructor(
    private val dishDao: DishDao
) : DishRepository {

    override fun observeDishes(): Flow<List<Dish>> = dishDao.observeAll().map { it.toDomainList() }

    override fun observeDishesByType(type: DishType): Flow<List<Dish>> =
        dishDao.observeByType(type.name).map { it.toDomainList() }

    override suspend fun getDish(id: String): Dish? = dishDao.getById(id)?.toDomain()

    override suspend fun upsert(dish: Dish) = dishDao.upsertWithIngredients(dish.toEntity(), dish.toIngredientEntities())

    override suspend fun delete(id: String) = dishDao.deleteById(id)

    override suspend fun isUsedInCalendar(id: String): Boolean = dishDao.countUsages(id) > 0

    private fun List<DishWithIngredients>.toDomainList(): List<Dish> = map(DishWithIngredients::toDomain)
}
