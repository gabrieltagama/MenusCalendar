package com.gabrieltagama.menuplanner.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Relation
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.model.Rating

/**
 * Row of the recipe book list: the columns of a dish the list shows and the names of its
 * ingredients, without description, preparation or quantities.
 */
internal data class DishListRow(
    val id: String,
    val name: String,
    val type: DishType,
    val heaviness: Heaviness,
    val rating: Rating,
    @ColumnInfo(name = "meal_time") val mealTime: MealTime,
    @Relation(parentColumn = "id", entityColumn = "dish_id", entity = IngredientEntity::class, projection = ["name"])
    val ingredientNames: List<String>
)
