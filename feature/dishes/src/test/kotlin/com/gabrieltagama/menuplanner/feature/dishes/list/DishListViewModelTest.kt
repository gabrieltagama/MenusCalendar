package com.gabrieltagama.menuplanner.feature.dishes.list

import app.cash.turbine.test
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.ObserveDishesUseCase
import com.gabrieltagama.menuplanner.feature.dishes.testing.FakeDishRepository
import com.gabrieltagama.menuplanner.feature.dishes.testing.MainDispatcherRule
import com.gabrieltagama.menuplanner.feature.dishes.testing.awaitItemMatching
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * DishListViewModel over the real ObserveDishesUseCase: loading, type and heaviness filters,
 * accent- and case-insensitive search by name or ingredient and alphabetical ordering.
 */
class DishListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val puree = dish("puree", "Puré de calabaza", DishType.STARTER)
    private val salad = dish("salad", "Ensalada mixta", DishType.STARTER)
    private val chicken = dish("chicken", "Pollo asado", DishType.MAIN)
    private val flan = dish("flan", "Flan de huevo", DishType.DESSERT)

    private val repository = FakeDishRepository(listOf(puree, salad, chicken, flan))
    private val viewModel by lazy { DishListViewModel(ObserveDishesUseCase(repository)) }

    @Test
    fun `loads every dish sorted by name`() = runTest {
        viewModel.uiState.test {
            val state = awaitItemMatching { !it.isLoading }

            assertTrue(state.hasDishes)
            assertEquals(listOf(salad, flan, chicken, puree), state.dishes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filters by type`() = runTest {
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }

            viewModel.onTypeSelect(DishType.STARTER)

            val state = awaitItemMatching { it.selectedType == DishType.STARTER }
            assertEquals(listOf(salad, puree), state.dishes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearing the type filter shows every dish again`() = runTest {
        viewModel.uiState.test {
            viewModel.onTypeSelect(DishType.DESSERT)
            awaitItemMatching { it.selectedType == DishType.DESSERT && !it.isLoading }

            viewModel.onTypeSelect(null)

            val state = awaitItemMatching { it.selectedType == null && it.dishes.size == 4 }
            assertEquals(4, state.dishes.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search ignores accents and case`() = runTest {
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }

            viewModel.onQueryChange("  PURE ")

            val state = awaitItemMatching { it.dishes.size == 1 }
            assertEquals(listOf(puree), state.dishes)
            assertEquals("  PURE ", viewModel.query.value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `accented query matches unaccented name`() = runTest {
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }

            viewModel.onQueryChange("flán")

            assertEquals(listOf(flan), awaitItemMatching { it.dishes.size == 1 }.dishes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search combines with type filter`() = runTest {
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }

            viewModel.onTypeSelect(DishType.MAIN)
            viewModel.onQueryChange("pu")

            val state = awaitItemMatching { it.selectedType == DishType.MAIN && it.dishes.isEmpty() }
            assertTrue(state.hasDishes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filters by heaviness combined with type`() = runTest {
        val lentils = Dish(id = "lentils", name = "Lentejas", type = DishType.SINGLE, heaviness = Heaviness.VERY_HIGH)
        repository.upsert(lentils)
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }

            viewModel.onHeavinessSelect(Heaviness.VERY_HIGH)
            viewModel.onTypeSelect(DishType.SINGLE)

            val state = awaitItemMatching { it.selectedHeaviness == Heaviness.VERY_HIGH && it.selectedType == DishType.SINGLE }
            assertEquals(listOf(lentils), state.dishes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search matches ingredient names`() = runTest {
        val omelette = Dish(
            id = "omelette",
            name = "Tortilla",
            ingredients = listOf(Ingredient("Patatas", 3.0, MeasureUnit.UNIT)),
            type = DishType.SINGLE,
            heaviness = Heaviness.MEDIUM
        )
        repository.upsert(omelette)
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }

            viewModel.onQueryChange("PATATA")

            val state = awaitItemMatching { it.query == "PATATA" }
            assertEquals(listOf(omelette), state.dishes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty repository reports no dishes`() = runTest {
        val emptyViewModel = DishListViewModel(ObserveDishesUseCase(FakeDishRepository()))

        emptyViewModel.uiState.test {
            val state = awaitItemMatching { !it.isLoading }

            assertEquals(false, state.hasDishes)
            assertTrue(state.dishes.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun dish(id: String, name: String, type: DishType) =
        Dish(id = id, name = name, type = type, heaviness = Heaviness.MEDIUM)
}
