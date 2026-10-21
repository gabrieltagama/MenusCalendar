package com.gabrieltagama.menuplanner.core.domain.usecase.dish

import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.common.RecipeError
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishRepository
import com.gabrieltagama.menuplanner.core.domain.fake.ImmediateTransactionRunner
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishDraft
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.MenuSlot
import com.gabrieltagama.menuplanner.core.domain.model.PlannedUse
import java.time.LocalDate
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
        useCase = SaveDishUseCase(repository, repository, TestDishes.fixedClock, TestDishes.idGenerator, StandardizeRecipeBookUseCase(repository), ImmediateTransactionRunner())
    }

    @Test
    fun `blank name fails with BlankDishName and is not persisted`() = runBlocking {
        val result = save(dish(name = "   "))

        assertEquals(Outcome.Failure(RecipeError.BlankDishName), result)
        assertTrue(repository.upserted.isEmpty())
    }

    @Test
    fun `ingredient with blank name fails with its index`() = runBlocking {
        val ingredients = listOf(
            Ingredient("Rice", 100.0, MeasureUnit.GRAM),
            Ingredient(" ", 1.0, MeasureUnit.UNIT)
        )

        val result = save(dish(ingredients = ingredients))

        assertEquals(Outcome.Failure(RecipeError.InvalidIngredient(1)), result)
        assertTrue(repository.upserted.isEmpty())
    }

    @Test
    fun `ingredient with negative quantity fails with its index`() = runBlocking {
        val ingredients = listOf(Ingredient("Salt", -0.5, MeasureUnit.PINCH))

        val result = save(dish(ingredients = ingredients))

        assertEquals(Outcome.Failure(RecipeError.InvalidIngredient(0)), result)
    }

    @Test
    fun `zero quantity is valid`() = runBlocking {
        val result = save(dish(ingredients = listOf(Ingredient("Pepper", 0.0, MeasureUnit.TO_TASTE))))

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

        val saved = (save(input) as Outcome.Success).value

        assertEquals("Paella", saved.name)
        assertEquals("Rice dish", saved.description)
        assertEquals("Cook it", saved.preparation)
        assertEquals(listOf(Ingredient("Rice", 200.0, MeasureUnit.GRAM)), saved.ingredients)
    }

    @Test
    fun `sets updatedAt from injected clock`() = runBlocking {
        val saved = (save(dish(updatedAt = TestDishes.OLD_INSTANT)) as Outcome.Success).value

        assertEquals(TestDishes.FIXED_INSTANT, saved.updatedAt)
    }

    @Test
    fun `persists normalized dish and preserves id`() = runBlocking {
        val input = dish(id = "fixed-id", name = " Soup ")

        val saved = (save(input) as Outcome.Success).value

        assertEquals("fixed-id", saved.id.value)
        assertEquals(listOf(saved), repository.upserted)
        assertEquals(saved, repository.current["fixed-id"])
    }

    @Test
    fun `new draft gets a generated id`() = runBlocking {
        val saved = (useCase(DishDraft(name = "Soup", type = DishType.STARTER, heaviness = Heaviness.VERY_LOW)) as Outcome.Success).value

        assertEquals(TestDishes.GENERATED_ID, saved.id)
        assertEquals(saved, repository.current[TestDishes.GENERATED_ID.value])
    }

    @Test
    fun `saving existing dish replaces it`() = runBlocking {
        val original: Dish = dish(id = "d1", name = "Old")
        repository.upsert(original)

        save(original.copy(name = "New"))

        assertEquals(1, repository.current.size)
        assertEquals("New", repository.current.getValue("d1").name)
    }

    @Test
    fun `changing the type of a dish planned in a lunch slot is refused`() = runBlocking {
        val planned = dish(id = "d1", type = DishType.STARTER)
        repository.upsert(planned)
        repository.plannedUses["d1"] = listOf(
            PlannedUse.Lunch(LocalDate.of(2026, 3, 12), MenuSlot.STARTER),
            PlannedUse.Lunch(LocalDate.of(2026, 3, 10), MenuSlot.STARTER)
        )

        val result = save(planned.copy(type = DishType.DESSERT))

        assertEquals(Outcome.Failure(RecipeError.DishPlannedIncompatibly(LocalDate.of(2026, 3, 10), days = 2)), result)
        assertEquals(DishType.STARTER, repository.current.getValue("d1").type)
    }

    @Test
    fun `making a planned dinner a lunch only dish is refused`() = runBlocking {
        val planned = dish(id = "d1", type = DishType.SINGLE)
        repository.upsert(planned)
        repository.plannedUses["d1"] = listOf(PlannedUse.Dinner(LocalDate.of(2026, 3, 10)))

        val result = save(planned.copy(mealTime = MealTime.LUNCH))

        assertEquals(Outcome.Failure(RecipeError.DishPlannedIncompatibly(LocalDate.of(2026, 3, 10), days = 1)), result)
    }

    @Test
    fun `changes that its planned days still accept are saved`() = runBlocking {
        val planned = dish(id = "d1", type = DishType.SINGLE)
        repository.upsert(planned)
        repository.plannedUses["d1"] = listOf(
            PlannedUse.Lunch(LocalDate.of(2026, 3, 10), MenuSlot.SINGLE),
            PlannedUse.Dinner(LocalDate.of(2026, 3, 11))
        )

        val result = save(planned.copy(name = "Renamed", mealTime = MealTime.DINNER))

        assertTrue(result is Outcome.Success)
        assertEquals("Renamed", repository.current.getValue("d1").name)
    }

    private suspend fun save(dish: Dish) = useCase(dish.toDraft())
}
