package com.gabrieltagama.menuplanner.feature.calendar.shopping

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDinnerPlanRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeLunchPlanRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakePlanQueries
import com.gabrieltagama.menuplanner.core.domain.fake.FakeShoppingChecklistRepository
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.LunchDay
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.ShoppingFilter
import com.gabrieltagama.menuplanner.core.domain.model.measure.IngredientKey
import com.gabrieltagama.menuplanner.core.domain.testing.MainDispatcherRule
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ClearExpiredShoppingChecksUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ClearShoppingChecksUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ObserveShoppingChecksUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ObserveShoppingListUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.SetShoppingItemCheckedUseCase
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ShoppingListViewModel over the real use cases and in-memory repositories: the default range of
 * the current and of another month, filtering by meals and dates (a range never ends before it
 * starts and one meal always stays selected), ticks kept per filter, bought products last and
 * left out of what is shared, and expired ticks cleared on opening. Robolectric because the route
 * is read with toRoute. Every view model scope is cancelled and joined after each test, so no
 * coroutine running on Dispatchers.Default resumes on Main while the rule resets it.
 */
@RunWith(AndroidJUnit4::class)
class ShoppingListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 10, 9)
    private val clock = Clock.fixed(Instant.parse("2026-10-09T10:00:00Z"), ZoneOffset.UTC)

    private val pastRice = dish("past", Ingredient("Garbanzos", 300.0, MeasureUnit.GRAM))
    private val rice = dish("rice", Ingredient("Arroz", 200.0, MeasureUnit.GRAM))
    private val omelette = dish("omelette", Ingredient("Huevo", 3.0, MeasureUnit.UNIT))

    private val lunchPlan = FakeLunchPlanRepository(
        listOf(singleDay(LocalDate.of(2026, 10, 5), pastRice), singleDay(LocalDate.of(2026, 10, 12), rice))
    )
    private val dinnerPlan = FakeDinnerPlanRepository(listOf(DinnerDay(LocalDate.of(2026, 10, 20), omelette, Instant.EPOCH)))

    private val viewModels = mutableListOf<ShoppingListViewModel>()

    @After
    fun cancelViewModels() = runBlocking {
        viewModels.forEach { it.viewModelScope.coroutineContext.job.cancelAndJoin() }
    }

    @Test
    fun currentMonthStartsTodayAndListsLunchesAndDinnersOfTheRange() = runBlocking {
        val viewModel = createViewModel()

        val state = awaitState(viewModel) { !it.isLoading }

        assertEquals(today, state.from)
        assertEquals(LocalDate.of(2026, 10, 31), state.to)
        assertEquals(listOf("Arroz", "Huevo"), state.items.map { it.name })
        assertTrue(state.canShare)
    }

    @Test
    fun anotherMonthStartsOnItsFirstDay() = runBlocking {
        val state = createViewModel(year = 2026, month = 11).uiState.value

        assertEquals(LocalDate.of(2026, 11, 1), state.from)
        assertEquals(LocalDate.of(2026, 11, 30), state.to)
    }

    @Test
    fun mealsFilterTheListAndTheLastMealCannotBeUnselected() = runBlocking {
        val viewModel = createViewModel()
        awaitState(viewModel) { !it.isLoading }

        viewModel.onMealToggle(Meal.DINNER)
        assertEquals(listOf("Arroz"), awaitState(viewModel) { it.selectedMeals == setOf(Meal.LUNCH) && !it.isLoading && it.items.size == 1 }.items.map { it.name })

        viewModel.onMealToggle(Meal.LUNCH)
        assertEquals(setOf(Meal.LUNCH), viewModel.uiState.value.selectedMeals)
    }

    @Test
    fun theRangeNeverEndsBeforeItStarts() = runBlocking {
        val viewModel = createViewModel()

        viewModel.onFromChange(LocalDate.of(2026, 11, 5))
        assertEquals(LocalDate.of(2026, 11, 5), viewModel.uiState.value.to)

        viewModel.onToChange(LocalDate.of(2026, 10, 1))
        assertEquals(LocalDate.of(2026, 10, 1), viewModel.uiState.value.from)
    }

    @Test
    fun ticksBelongToTheFilterAndBoughtProductsGoLastAndAreNotShared() = runBlocking {
        val viewModel = createViewModel()
        val arroz = awaitState(viewModel) { !it.isLoading }.items.first { it.name == "Arroz" }

        viewModel.onItemCheck(arroz, isChecked = true)
        val checked = awaitState(viewModel) { it.isChecked(arroz) }
        assertEquals(listOf("Huevo", "Arroz"), checked.sortedItems.map { it.name })
        assertEquals(listOf("Huevo"), checked.pendingItems.map { it.name })
        assertTrue(checked.hasChecked)

        viewModel.onMealToggle(Meal.DINNER)
        assertFalse(awaitState(viewModel) { it.selectedMeals == setOf(Meal.LUNCH) && it.checked.isEmpty() }.isChecked(arroz))

        viewModel.onMealToggle(Meal.DINNER)
        assertTrue(awaitState(viewModel) { it.selectedMeals == Meal.entries.toSet() && it.isChecked(arroz) }.isChecked(arroz))
    }

    @Test
    fun clearingTheTicksUnchecksEverythingOfTheFilter() = runBlocking {
        val viewModel = createViewModel()
        val arroz = awaitState(viewModel) { !it.isLoading }.items.first { it.name == "Arroz" }
        viewModel.onItemCheck(arroz, isChecked = true)
        awaitState(viewModel) { it.isChecked(arroz) }

        viewModel.onClearChecks()

        assertFalse(awaitState(viewModel) { it.checked.isEmpty() }.hasChecked)
    }

    @Test
    fun ticksOfRangesAlreadyOverAreClearedOnOpening() = runBlocking {
        val expired = ShoppingFilter(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))
        val checklist = FakeShoppingChecklistRepository(mapOf(expired to setOf(IngredientKey.of("arroz"))))

        val viewModel = createViewModel(checklist = checklist)
        awaitState(viewModel) { !it.isLoading }

        assertFalse(withTimeout(TIMEOUT_MS) { checklist.checkedByFilter.first { expired !in it } }.containsKey(expired))
    }

    private fun createViewModel(
        year: Int = 2026,
        month: Int = 10,
        checklist: FakeShoppingChecklistRepository = FakeShoppingChecklistRepository()
    ) = ShoppingListViewModel(
        savedStateHandle = SavedStateHandle(mapOf("year" to year, "month" to month)),
        clock = clock,
        observeShoppingChecks = ObserveShoppingChecksUseCase(checklist),
        observeShoppingList = ObserveShoppingListUseCase(FakePlanQueries(lunchPlan, dinnerPlan)),
        clearExpiredShoppingChecks = ClearExpiredShoppingChecksUseCase(checklist, clock),
        setShoppingItemChecked = SetShoppingItemCheckedUseCase(checklist),
        clearShoppingChecks = ClearShoppingChecksUseCase(checklist),
        defaultDispatcher = Dispatchers.Default
    ).also(viewModels::add)

    private suspend fun awaitState(
        viewModel: ShoppingListViewModel,
        predicate: (ShoppingListUiState) -> Boolean
    ): ShoppingListUiState = withTimeout(TIMEOUT_MS) { viewModel.uiState.first(predicate) }

    private fun dish(id: String, ingredient: Ingredient) =
        Dish(id = DishId(id), name = id, ingredients = listOf(ingredient), type = DishType.SINGLE, heaviness = Heaviness.MEDIUM, updatedAt = Instant.EPOCH)

    private fun singleDay(date: LocalDate, dish: Dish) = LunchDay(date = date, menu = DailyMenu.Single(single = dish), updatedAt = Instant.EPOCH)

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
