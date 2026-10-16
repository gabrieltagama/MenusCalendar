package com.gabrieltagama.menuplanner.core.ui.preview

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import java.time.Instant

/**
 * Sample dishes for Compose previews: the name is also the id and the time is fixed, so previews
 * never depend on randomness or the clock.
 */
object PreviewDishes {
    fun dish(
        name: String,
        type: DishType,
        heaviness: Heaviness = Heaviness.MEDIUM,
        mealTime: MealTime = MealTime.ANY,
        ingredients: List<Ingredient> = emptyList()
    ): Dish = Dish(
        id = DishId(name),
        name = name,
        ingredients = ingredients,
        type = type,
        heaviness = heaviness,
        mealTime = mealTime,
        updatedAt = Instant.EPOCH
    )
}
