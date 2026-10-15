package com.gabrieltagama.menuplanner.core.domain.model.measure

import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Tests the conversion of kitchen measures to grams, millilitres or units, the grouping key of
 * ingredient names and that a product keeps one unit across the whole recipe book.
 */
class IngredientStandardizerTest {

    private fun standard(vararg lines: Ingredient) = IngredientStandardizer.standardizeLines(lines.toList())

    @Test
    fun `scales kilograms and litres`() {
        assertEquals(
            listOf(Ingredient("Arroz", 400.0, MeasureUnit.GRAM), Ingredient("Leche", 1500.0, MeasureUnit.MILLILITER)),
            standard(Ingredient("Arroz", 0.4, MeasureUnit.KILOGRAM), Ingredient("Leche", 1.5, MeasureUnit.LITER))
        )
    }

    @Test
    fun `tablespoons are 20 g for solids and 15 ml for liquids`() {
        assertEquals(
            listOf(Ingredient("Tomate frito", 40.0, MeasureUnit.GRAM), Ingredient("Aceite de oliva", 45.0, MeasureUnit.MILLILITER)),
            standard(Ingredient("Tomate frito", 2.0, MeasureUnit.TABLESPOON), Ingredient("Aceite de oliva", 3.0, MeasureUnit.TABLESPOON))
        )
    }

    @Test
    fun `unknown liquid is detected from its other lines`() {
        assertEquals(
            listOf(Ingredient("Salsa de soja", 15.0, MeasureUnit.MILLILITER), Ingredient("Salsa de soja", 50.0, MeasureUnit.MILLILITER)),
            standard(Ingredient("Salsa de soja", 1.0, MeasureUnit.TABLESPOON), Ingredient("Salsa de soja", 50.0, MeasureUnit.MILLILITER))
        )
    }

    @Test
    fun `pinch and to taste use the catalog amount or 10 when unknown`() {
        assertEquals(
            listOf(
                Ingredient("Sal", 5.0, MeasureUnit.GRAM),
                Ingredient("Aceite para freír", 250.0, MeasureUnit.MILLILITER),
                Ingredient("Comino", 20.0, MeasureUnit.GRAM)
            ),
            standard(
                Ingredient("Sal", 1.0, MeasureUnit.PINCH),
                Ingredient("Aceite para freír", 0.0, MeasureUnit.TO_TASTE),
                Ingredient("Comino", 2.0, MeasureUnit.PINCH)
            )
        )
    }

    @Test
    fun `units stay as units when the product is only counted`() {
        val lines = arrayOf(Ingredient("Dientes de ajo", 3.0, MeasureUnit.UNIT), Ingredient("Diente de ajo", 1.0, MeasureUnit.UNIT))

        assertEquals(lines.toList(), standard(*lines))
    }

    @Test
    fun `units become grams with the estimated weight when the product is also weighed`() {
        assertEquals(
            listOf(Ingredient("Cebollas", 300.0, MeasureUnit.GRAM), Ingredient("Cebolla", 100.0, MeasureUnit.GRAM)),
            standard(Ingredient("Cebollas", 2.0, MeasureUnit.UNIT), Ingredient("Cebolla", 100.0, MeasureUnit.GRAM))
        )
        assertEquals(
            listOf(Ingredient("Nabo", 100.0, MeasureUnit.GRAM), Ingredient("Nabo", 50.0, MeasureUnit.GRAM)),
            standard(Ingredient("Nabo", 1.0, MeasureUnit.UNIT), Ingredient("Nabo", 50.0, MeasureUnit.GRAM))
        )
    }

    @Test
    fun `a product keeps one unit across every dish of the recipe book`() {
        val tortilla = dish(id = "t", ingredients = listOf(Ingredient("Huevos", 4.0, MeasureUnit.UNIT)))
        val flan = dish(id = "f", ingredients = listOf(Ingredient("Huevo", 120.0, MeasureUnit.GRAM)))

        val standardTortilla = IngredientStandardizer.standardize(listOf(tortilla, flan)).first()

        assertEquals(listOf(Ingredient("Huevos", 240.0, MeasureUnit.GRAM)), standardTortilla.ingredients)
    }

    @Test
    fun `standardizing twice changes nothing`() {
        val dishes = listOf(
            dish(id = "a", ingredients = listOf(Ingredient("Aceite", 2.0, MeasureUnit.TABLESPOON), Ingredient("Patatas", 3.0, MeasureUnit.UNIT))),
            dish(id = "b", ingredients = listOf(Ingredient("aceite", 0.1, MeasureUnit.LITER), Ingredient("Patata", 0.5, MeasureUnit.KILOGRAM)))
        )
        val once = IngredientStandardizer.standardize(dishes)

        assertEquals(once, IngredientStandardizer.standardize(once))
        assertEquals(setOf(MeasureUnit.MILLILITER, MeasureUnit.GRAM), once.flatMap { it.ingredients }.map { it.unit }.toSet())
    }

    @Test
    fun `key ignores case accents spaces and plurals but not different products`() {
        assertEquals(IngredientKey.of(" Calabacines "), IngredientKey.of("calabacín"))
        assertEquals(IngredientKey.of("Pimientos  rojos"), IngredientKey.of("pimiento rojo"))
        assertEquals(IngredientKey.of("Tomates"), IngredientKey.of("tomate"))
        assertNotEquals(IngredientKey.of("Sal"), IngredientKey.of("Sal gruesa"))
    }
}
