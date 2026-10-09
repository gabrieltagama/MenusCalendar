package com.gabrieltagama.menuplanner.core.domain.usecase.dish

import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishRepository
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Verifies that SaveDishUseCase rejects non-finite ingredient quantities (NaN and infinities)
 * with the index of the offending line and does not persist the dish.
 */
class SaveDishNonFiniteQuantityTest {
    private lateinit var repository: FakeDishRepository
    private lateinit var useCase: SaveDishUseCase

    @Before
    fun setUp() {
        repository = FakeDishRepository()
        useCase = SaveDishUseCase(repository, TestDishes.fixedClock)
    }

    @Test
    fun `NaN quantity fails with its index`() = runBlocking {
        val ingredients = listOf(
            Ingredient("Rice", 100.0, MeasureUnit.GRAM),
            Ingredient("Oil", Double.NaN, MeasureUnit.TABLESPOON)
        )

        val result = useCase(dish(ingredients = ingredients))

        assertEquals(Outcome.Failure(DomainError.InvalidIngredient(1)), result)
        assertTrue(repository.upserted.isEmpty())
    }

    @Test
    fun `positive infinity quantity fails with its index`() = runBlocking {
        val ingredients = listOf(Ingredient("Water", Double.POSITIVE_INFINITY, MeasureUnit.LITER))

        val result = useCase(dish(ingredients = ingredients))

        assertEquals(Outcome.Failure(DomainError.InvalidIngredient(0)), result)
        assertTrue(repository.upserted.isEmpty())
    }

    @Test
    fun `negative infinity quantity fails with its index`() = runBlocking {
        val ingredients = listOf(
            Ingredient("Salt", 1.0, MeasureUnit.PINCH),
            Ingredient("Sugar", 2.0, MeasureUnit.GRAM),
            Ingredient("Flour", Double.NEGATIVE_INFINITY, MeasureUnit.GRAM)
        )

        val result = useCase(dish(ingredients = ingredients))

        assertEquals(Outcome.Failure(DomainError.InvalidIngredient(2)), result)
    }

    @Test
    fun `first invalid line is reported when several are non-finite`() = runBlocking {
        val ingredients = listOf(
            Ingredient("Milk", Double.POSITIVE_INFINITY, MeasureUnit.MILLILITER),
            Ingredient("Egg", Double.NaN, MeasureUnit.UNIT)
        )

        val result = useCase(dish(ingredients = ingredients))

        assertEquals(Outcome.Failure(DomainError.InvalidIngredient(0)), result)
    }
}
