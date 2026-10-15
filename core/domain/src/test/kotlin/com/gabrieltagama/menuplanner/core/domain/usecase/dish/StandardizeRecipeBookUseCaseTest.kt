package com.gabrieltagama.menuplanner.core.domain.usecase.dish

import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishRepository
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests that the recipe book is stored in standard units: only changed dishes are written, they
 * keep their updatedAt, and saving a dish converts it and the other dishes that share a product.
 */
class StandardizeRecipeBookUseCaseTest {
    private val standardDish = dish(id = "ok", ingredients = listOf(Ingredient("Arroz", 300.0, MeasureUnit.GRAM)))
    private val legacyDish = dish(id = "old", ingredients = listOf(Ingredient("Aceite de oliva", 2.0, MeasureUnit.TABLESPOON)))

    @Test
    fun `converts only the dishes that change and keeps their updatedAt`() = runBlocking {
        val repository = FakeDishRepository(listOf(standardDish, legacyDish))

        StandardizeRecipeBookUseCase(repository)()

        val converted = legacyDish.copy(ingredients = listOf(Ingredient("Aceite de oliva", 30.0, MeasureUnit.MILLILITER)))
        assertEquals(listOf(converted), repository.upserted)
        assertEquals(listOf(listOf(converted)), repository.batches)
        assertEquals(TestDishes.OLD_INSTANT, repository.current.getValue("old").updatedAt)
    }

    @Test
    fun `does nothing when everything is already standard`() = runBlocking {
        val repository = FakeDishRepository(listOf(standardDish))

        StandardizeRecipeBookUseCase(repository)()

        assertTrue(repository.upserted.isEmpty())
    }

    @Test
    fun `saving a weighed product converts the dishes that only counted it`() = runBlocking {
        val tortilla = dish(id = "tortilla", ingredients = listOf(Ingredient("Huevos", 4.0, MeasureUnit.UNIT)))
        val repository = FakeDishRepository(listOf(tortilla))
        val saveDish = SaveDishUseCase(TestDishes.fixedClock, StandardizeRecipeBookUseCase(repository))

        val saved = saveDish(dish(id = "flan", ingredients = listOf(Ingredient("Huevo", 0.12, MeasureUnit.KILOGRAM)))) as Outcome.Success

        assertEquals(listOf(Ingredient("Huevo", 120.0, MeasureUnit.GRAM)), saved.value.ingredients)
        assertEquals(listOf(Ingredient("Huevos", 240.0, MeasureUnit.GRAM)), repository.current.getValue("tortilla").ingredients)
    }
}
