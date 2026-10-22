package com.gabrieltagama.menuplanner.core.data.repository

import com.gabrieltagama.menuplanner.core.data.database.dao.DishDao
import com.gabrieltagama.menuplanner.core.data.database.entity.DishWithIngredients
import com.gabrieltagama.menuplanner.core.data.mapper.toDomain
import com.gabrieltagama.menuplanner.core.data.mapper.toEntity
import com.gabrieltagama.menuplanner.core.data.mapper.toIngredientEntities
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.port.DishRepository
import javax.inject.Inject

/**
 * Room-backed recipe book (write side and the reads that commands need). Saving a dish replaces its
 * ingredient lines atomically, saving several dishes is one transaction and deleting a dish checks
 * that it is not planned in the same transaction. The screens read through RoomDishQueries.
 */
internal class RoomDishRepository @Inject constructor(
    private val dishDao: DishDao
) : DishRepository {

    override suspend fun getDishes(): List<Dish> = dishDao.getAll().map(DishWithIngredients::toDomain)

    override suspend fun getDish(id: DishId): Dish? = dishDao.getById(id.value)?.toDomain()

    override suspend fun upsert(dish: Dish) = dishDao.upsertWithIngredients(dish.toEntity(), dish.toIngredientEntities())

    override suspend fun upsertAll(dishes: List<Dish>) =
        dishDao.upsertAllWithIngredients(dishes.map { it.toEntity() to it.toIngredientEntities() })

    override suspend fun deleteIfUnused(id: DishId): Boolean = dishDao.deleteIfUnused(id.value)
}
