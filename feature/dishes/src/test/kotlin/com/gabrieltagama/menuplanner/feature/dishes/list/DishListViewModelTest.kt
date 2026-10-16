package com.gabrieltagama.menuplanner.feature.dishes.list

import app.cash.turbine.test
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishUsageRepository
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.DishSort
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.Rating
import com.gabrieltagama.menuplanner.core.domain.testing.MainDispatcherRule
import com.gabrieltagama.menuplanner.core.domain.testing.awaitItemMatching
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.ObserveDishUsageUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.ObserveDishesUseCase
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * DishListViewModel over the real ObserveDishesUseCase: loading, multiple-selection type,
 * heaviness, rating and meal time filters, accent- and case-insensitive search by name or
 * ingredient, alphabetical ordering and the order by last time served.
 */
class DishListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val puree = dish("puree", "Puré de calabaza", DishType.STARTER)
    private val salad = dish("salad", "Ensalada mixta", DishType.STARTER)
    private val chicken = dish("chicken", "Pollo asado", DishType.MAIN)
    private val flan = dish("flan", "Flan de huevo", DishType.DESSERT)

    private val repository = FakeDishRepository(listOf(puree, salad, chicken, flan))
    private val usageRepository = FakeDishUsageRepository(
        mapOf(
            puree.id to listOf(LocalDate.of(2026, 1, 10)),
            chicken.id to listOf(LocalDate.of(2025, 12, 1), LocalDate.of(2026, 2, 1)),
            salad.id to listOf(LocalDate.of(2026, 1, 14))
        )
    )
    private val viewModel by lazy { DishListViewModel(ObserveDishesUseCase(repository), ObserveDishUsageUseCase(usageRepository, TestDishes.fixedClock)) }

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

            viewModel.onTypesChange(setOf(DishType.STARTER))

            val state = awaitItemMatching { it.selectedTypes == setOf(DishType.STARTER) }
            assertEquals(listOf(salad, puree), state.dishes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearing the type filter shows every dish again`() = runTest {
        viewModel.uiState.test {
            viewModel.onTypesChange(setOf(DishType.DESSERT))
            awaitItemMatching { it.selectedTypes == setOf(DishType.DESSERT) && !it.isLoading }

            viewModel.onTypesChange(emptySet())

            val state = awaitItemMatching { it.selectedTypes.isEmpty() && it.dishes.size == 4 }
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

            viewModel.onTypesChange(setOf(DishType.MAIN))
            viewModel.onQueryChange("pu")

            val state = awaitItemMatching { it.selectedTypes == setOf(DishType.MAIN) && it.dishes.isEmpty() }
            assertTrue(state.hasDishes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filters by heaviness combined with type`() = runTest {
        val lentils = Dish(id = DishId("lentils"), name = "Lentejas", type = DishType.SINGLE, heaviness = Heaviness.VERY_HIGH, updatedAt = Instant.EPOCH)
        repository.upsert(lentils)
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }

            viewModel.onHeavinessChange(setOf(Heaviness.VERY_HIGH))
            viewModel.onTypesChange(setOf(DishType.SINGLE))

            val state = awaitItemMatching { it.selectedHeaviness == setOf(Heaviness.VERY_HIGH) && it.selectedTypes == setOf(DishType.SINGLE) }
            assertEquals(listOf(lentils), state.dishes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filters by several ratings and meal times`() = runTest {
        val stew = Dish(id = DishId("stew"), name = "Cocido", type = DishType.SINGLE, heaviness = Heaviness.VERY_HIGH, rating = Rating.FIVE, mealTime = MealTime.ANY, updatedAt = Instant.EPOCH)
        val soup = Dish(id = DishId("soup"), name = "Sopa", type = DishType.STARTER, heaviness = Heaviness.VERY_LOW, rating = Rating.FOUR, mealTime = MealTime.DINNER, updatedAt = Instant.EPOCH)
        val toast = Dish(id = DishId("toast"), name = "Tostada", type = DishType.SINGLE, heaviness = Heaviness.VERY_LOW, rating = Rating.FOUR, mealTime = MealTime.LUNCH, updatedAt = Instant.EPOCH)
        listOf(stew, soup, toast).forEach { repository.upsert(it) }
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }

            viewModel.onRatingsChange(setOf(Rating.FOUR, Rating.FIVE))
            viewModel.onMealTimesChange(setOf(MealTime.DINNER, MealTime.ANY))

            val state = awaitItemMatching {
                it.selectedRatings == setOf(Rating.FOUR, Rating.FIVE) && it.selectedMealTimes == setOf(MealTime.DINNER, MealTime.ANY)
            }
            assertEquals(listOf(stew, soup), state.dishes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search matches ingredient names`() = runTest {
        val omelette = Dish(
            id = DishId("omelette"),
            name = "Tortilla",
            ingredients = listOf(Ingredient("Patatas", 3.0, MeasureUnit.UNIT)),
            type = DishType.SINGLE,
            heaviness = Heaviness.MEDIUM,
            updatedAt = Instant.EPOCH
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
        val emptyViewModel = DishListViewModel(ObserveDishesUseCase(FakeDishRepository()), ObserveDishUsageUseCase(FakeDishUsageRepository(), TestDishes.fixedClock))

        emptyViewModel.uiState.test {
            val state = awaitItemMatching { !it.isLoading }

            assertEquals(false, state.hasDishes)
            assertTrue(state.dishes.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sorting by last served puts never served first and then the oldest`() = runTest {
        viewModel.uiState.test {
            awaitItemMatching { !it.isLoading }

            viewModel.onSortChange(DishSort.LAST_SERVED)

            val state = awaitItemMatching { it.sort == DishSort.LAST_SERVED }
            assertEquals(listOf(flan, chicken, puree, salad), state.dishes)
            assertEquals(LocalDate.of(2025, 12, 1), state.usage.lastServedOf(chicken.id))
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun dish(id: String, name: String, type: DishType) =
        Dish(id = DishId(id), name = name, type = type, heaviness = Heaviness.MEDIUM, updatedAt = Instant.EPOCH)
}
