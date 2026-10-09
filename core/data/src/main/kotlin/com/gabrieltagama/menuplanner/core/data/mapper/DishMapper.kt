package com.gabrieltagama.menuplanner.core.data.mapper

import com.gabrieltagama.menuplanner.core.data.database.entity.DishEntity
import com.gabrieltagama.menuplanner.core.data.database.entity.DishWithIngredients
import com.gabrieltagama.menuplanner.core.data.database.entity.IngredientEntity
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import java.time.Instant

/**
 * Maps dishes between Room rows and the domain. Enums are stored by name and the ingredient
 * order is preserved through the position column.
 */
internal fun DishWithIngredients.toDomain(): Dish = Dish(
    id = dish.id,
    name = dish.name,
    description = dish.description,
    ingredients = ingredients.sortedBy(IngredientEntity::position).map(IngredientEntity::toDomain),
    preparation = dish.preparation,
    type = DishType.valueOf(dish.type),
    heaviness = Heaviness.valueOf(dish.heaviness),
    updatedAt = Instant.ofEpochMilli(dish.updatedAt)
)

internal fun Dish.toEntity(): DishEntity = DishEntity(
    id = id,
    name = name,
    description = description,
    preparation = preparation,
    type = type.name,
    heaviness = heaviness.name,
    updatedAt = updatedAt.toEpochMilli()
)

internal fun Dish.toIngredientEntities(): List<IngredientEntity> =
    ingredients.mapIndexed { index, ingredient ->
        IngredientEntity(
            dishId = id,
            position = index,
            name = ingredient.name,
            quantity = ingredient.quantity,
            unit = ingredient.unit.name
        )
    }

private fun IngredientEntity.toDomain(): Ingredient =
    Ingredient(name = name, quantity = quantity, unit = MeasureUnit.valueOf(unit))
