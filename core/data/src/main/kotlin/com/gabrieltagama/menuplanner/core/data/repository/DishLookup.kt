package com.gabrieltagama.menuplanner.core.data.repository

import com.gabrieltagama.menuplanner.core.data.database.dao.DishDao
import com.gabrieltagama.menuplanner.core.data.database.entity.DishWithIngredients
import com.gabrieltagama.menuplanner.core.data.mapper.toDomain
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId

/**
 * Loads the dishes referenced by planned days with one query, shared by the lunch and dinner
 * repositories.
 */
internal suspend fun DishDao.dishesById(ids: List<String>): Map<DishId, Dish> =
    if (ids.isEmpty()) emptyMap() else getByIds(ids).map(DishWithIngredients::toDomain).associateBy(Dish::id)
