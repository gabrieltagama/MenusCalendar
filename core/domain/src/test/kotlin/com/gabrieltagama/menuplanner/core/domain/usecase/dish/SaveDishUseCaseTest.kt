package com.gabrieltagama.menuplanner.core.domain.usecase.dish

import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishRepository
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests validation, normalization and persistence of SaveDishUseCase.
 */
class SaveDishUseCaseTest {
    private lateinit var repository: FakeDishRepository
    private lateinit var useCase: SaveDishUseCase

    @Before
    fun setUp() {
        repository = FakeDishRepository()
        useCase = SaveDishUseCase(repository, TestDishes.fixedClock)
    }

    @Test
    fun `blank name fails with BlankDishName and is not persisted`() = runBlocking {
        val result = useCase(dish(name = "   "))

        assertEquals(Outcome.Failure(DomainError.BlankDishName), result)
        assertTrue(repository.upserted.isEmpty())
    }

    @Test
    fun `ingredient with blank name fails with its index`() = runBlocking {
        val ingredients = listOf(
            Ingredient("Rice", 100.0, MeasureUnit.GRAM),
            Ingredient(" ", 1.0, MeasureUnit.UNIT)
        )

        val result = useCase(dish(ingredients = ingredients))

        assertEquals(Outcome.Failure(DomainError.InvalidIngredient(1)), result)
        assertTrue(repository.upserted.isEmpty())
    }

    @Test
    fun `ingredient with negative quantity fails with its index`() = runBlocking {
        val ingredients = listOf(Ingredient("Salt", -0.5, MeasureUnit.PINCH))

        val result = useCase(dish(ingredients = ingredients))

        assertEquals(Outcome.Failure(DomainError.InvalidIngredient(0)), result)
    }

    @Test
    fun `zero quantity is valid`() = runBlocking {
        val result = useCase(dish(ingredients = listOf(Ingredient("Pepper", 0.0, MeasureUnit.TO_TASTE))))

        assertTrue(result is Outcome.Success)
    }

    @Test
    fun `trims name description preparation and ingredient names`() = runBlocking {
        val input = dish(
            name = "  Paella ",
            description = " Rice dish  ",
            preparation = "\n Cook it \t",
            ingredients = listOf(Ingredient("  Rice ", 200.0, MeasureUnit.GRAM))
        )

        val saved = (useCase(input) as Outcome.Success).value

        assertEquals("Paella", saved.name)
        assertEquals("Rice dish", saved.description)
        assertEquals("Cook it", saved.preparation)
        assertEquals(listOf(Ingredient("Rice", 200.0, MeasureUnit.GRAM)), saved.ingredients)
    }

    @Test
    fun `sets updatedAt from injected clock`() = runBlocking {
        val saved = (useCase(dish(updatedAt = TestDishes.OLD_INSTANT)) as Outcome.Success).value

        assertEquals(TestDishes.FIXED_INSTANT, saved.updatedAt)
    }

    @Test
    fun `persists normalized dish and preserves id`() = runBlocking {
        val input = dish(id = "fixed-id", name = " Soup ")

        val saved = (useCase(input) as Outcome.Success).value

        assertEquals("fixed-id", saved.id)
        assertEquals(listOf(saved), repository.upserted)
        assertEquals(saved, repository.current["fixed-id"])
    }

    @Test
    fun `saving existing dish replaces it`() = runBlocking {
        val original: Dish = dish(id = "d1", name = "Old")
        repository.upsert(original)

        useCase(original.copy(name = "New"))

        assertEquals(1, repository.current.size)
        assertEquals("New", repository.current.getValue("d1").name)
    }
}
