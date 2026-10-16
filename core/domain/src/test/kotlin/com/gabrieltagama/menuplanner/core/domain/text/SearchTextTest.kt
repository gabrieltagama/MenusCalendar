package com.gabrieltagama.menuplanner.core.domain.text

import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Dish search ignores case and accents and also matches ingredient names.
 */
class SearchTextTest {
    private val puree = dish(name = "Puré de calabaza", ingredients = listOf(Ingredient("Cebolla", 100.0, MeasureUnit.GRAM)))

    @Test
    fun `matches name without accents or case`() {
        assertTrue(puree.matchesSearch("PURE"))
        assertTrue(puree.matchesSearch("  calabaza "))
    }

    @Test
    fun `matches ingredient names`() {
        assertTrue(puree.matchesSearch("cebolla"))
    }

    @Test
    fun `empty query matches and unrelated text does not`() {
        assertTrue(puree.matchesSearch(""))
        assertFalse(puree.matchesSearch("lentejas"))
    }
}
