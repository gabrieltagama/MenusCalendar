package com.gabrieltagama.menuplanner.core.domain.model

import com.gabrieltagama.menuplanner.core.domain.common.RecipeError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Rules shared by the whole app for a dish: DishName and IngredientName trim the text and reject it
 * when empty, Quantity accepts only finite non-negative numbers (also when parsed from typed text),
 * and Dish.validate applies them.
 */
class ValidationRulesTest {

    @Test
    fun `dish name is trimmed and cannot be empty`() {
        assertEquals("Paella", DishName.of("  Paella ")?.value)
        assertNull(DishName.of("   "))
        assertFalse(DishName.isValid(""))
    }

    @Test
    fun `ingredient name is trimmed and cannot be empty`() {
        assertEquals("Arroz", IngredientName.of(" Arroz ")?.value)
        assertNull(IngredientName.of(" "))
    }

    @Test
    fun `quantity must be finite and not negative`() {
        assertEquals(1.5, Quantity.of(1.5)?.value)
        assertEquals(Quantity.ZERO, Quantity.of(0.0))
        listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { assertFalse(it.toString(), Quantity.isValid(it)) }
    }

    @Test
    fun `quantity is parsed from what the user typed`() {
        assertEquals(1.5, Quantity.parse(" 1,5 ", MeasureUnit.GRAM)?.value)
        assertEquals(0.25, Quantity.parse("0.25", MeasureUnit.MILLILITER)?.value)
        assertEquals(Quantity.ZERO, Quantity.parse("", MeasureUnit.TO_TASTE))
        assertNull(Quantity.parse("", MeasureUnit.GRAM))
        assertNull(Quantity.parse("1,2,3", MeasureUnit.GRAM))
        assertNull(Quantity.parse("-2", MeasureUnit.UNIT))
    }

    @Test
    fun `dish validation uses the shared rules`() {
        val valid = Ingredient("Arroz", 100.0, MeasureUnit.GRAM)

        assertNull(Dish.validate("Paella", listOf(valid)))
        assertEquals(RecipeError.BlankDishName, Dish.validate("  ", listOf(valid)))
        assertEquals(
            RecipeError.InvalidIngredient(1),
            Dish.validate("Paella", listOf(valid, valid.copy(quantity = -1.0)))
        )
    }
}
