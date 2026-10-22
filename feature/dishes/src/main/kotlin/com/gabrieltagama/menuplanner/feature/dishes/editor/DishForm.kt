package com.gabrieltagama.menuplanner.feature.dishes.editor

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishDraft
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.DishName
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.IngredientName
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.Quantity
import com.gabrieltagama.menuplanner.core.domain.model.Rating
import kotlinx.serialization.Serializable

/**
 * What the user edits in the dish form, kept as typed: quantities stay as text ("1,5", "0.25")
 * until saving (Quantity.parse), and each ingredient row keeps a stable key for the list. It is the
 * only copy of the dish fields on the screen side: of fills it from a saved dish, toDraft turns it
 * into the DishDraft that is saved (null while a quantity is not a valid number) and, being
 * serializable, DishDraftStore keeps it so a recipe being typed survives the system killing the app.
 */
@Serializable
data class DishForm(
    val name: String = "",
    val description: String = "",
    val type: DishType = DishType.MAIN,
    val heaviness: Heaviness = Heaviness.MEDIUM,
    val rating: Rating = Rating.THREE,
    val mealTime: MealTime = MealTime.ANY,
    val ingredients: List<IngredientForm> = emptyList(),
    val preparation: String = ""
) {
    val isNameValid: Boolean get() = DishName.isValid(name)

    val nextIngredientKey: Long get() = (ingredients.maxOfOrNull(IngredientForm::key) ?: -1L) + 1

    fun toDraft(id: DishId?): DishDraft? {
        val lines = ingredients.map(IngredientForm::toDomain)
        if (null in lines) return null
        return DishDraft(
            id = id,
            name = name,
            description = description,
            ingredients = lines.filterNotNull(),
            preparation = preparation,
            type = type,
            heaviness = heaviness,
            rating = rating,
            mealTime = mealTime
        )
    }

    companion object {
        fun of(dish: Dish): DishForm = DishForm(
            name = dish.name,
            description = dish.description,
            type = dish.type,
            heaviness = dish.heaviness,
            rating = dish.rating,
            mealTime = dish.mealTime,
            ingredients = dish.ingredients.mapIndexed { index, ingredient -> ingredient.toForm(index.toLong()) },
            preparation = dish.preparation
        )
    }
}

@Serializable
data class IngredientForm(
    val key: Long,
    val name: String = "",
    val quantityText: String = "",
    val unit: MeasureUnit = MeasureUnit.GRAM
) {
    val parsedQuantity: Double? get() = Quantity.parse(quantityText, unit)?.value

    val isNameValid: Boolean get() = IngredientName.isValid(name)

    val isQuantityValid: Boolean get() = parsedQuantity != null

    fun toDomain(): Ingredient? = parsedQuantity?.let { Ingredient(name = name, quantity = it, unit = unit) }
}

private fun Ingredient.toForm(key: Long): IngredientForm =
    IngredientForm(key = key, name = name, quantityText = quantity.toEditableText(), unit = unit)

private fun Double.toEditableText(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString().replace('.', ',')
