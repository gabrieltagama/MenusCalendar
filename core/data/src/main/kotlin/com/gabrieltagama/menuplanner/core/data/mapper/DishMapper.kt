package com.gabrieltagama.menuplanner.core.data.mapper

import com.gabrieltagama.menuplanner.core.data.database.entity.DishEntity
import com.gabrieltagama.menuplanner.core.data.database.entity.DishWithIngredients
import com.gabrieltagama.menuplanner.core.data.database.entity.IngredientEntity
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient

/**
 * Maps dishes between Room rows and the domain (column types are converted by Room, see
 * Converters); the ingredient order is preserved through the position column.
 */
internal fun DishWithIngredients.toDomain(): Dish = Dish(
    id = DishId(dish.id),
    name = dish.name,
    description = dish.description,
    ingredients = ingredients.sortedBy(IngredientEntity::position).map(IngredientEntity::toDomain),
    preparation = dish.preparation,
    type = dish.type,
    heaviness = dish.heaviness,
    rating = dish.rating,
    mealTime = dish.mealTime,
    updatedAt = dish.updatedAt
)

internal fun Dish.toEntity(): DishEntity = DishEntity(
    id = id.value,
    name = name,
    description = description,
    preparation = preparation,
    type = type,
    heaviness = heaviness,
    rating = rating,
    mealTime = mealTime,
    updatedAt = updatedAt
)

internal fun Dish.toIngredientEntities(): List<IngredientEntity> =
    ingredients.mapIndexed { index, ingredient ->
        IngredientEntity(
            dishId = id.value,
            position = index,
            name = ingredient.name,
            quantity = ingredient.quantity,
            unit = ingredient.unit
        )
    }

private fun IngredientEntity.toDomain(): Ingredient =
    Ingredient(name = name, quantity = quantity, unit = unit)
