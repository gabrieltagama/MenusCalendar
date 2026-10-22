package com.gabrieltagama.menuplanner.feature.calendar.dinner

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDinnerPlanRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishUsageQueries
import com.gabrieltagama.menuplanner.core.domain.fake.TestPlanCommands
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.testing.MainDispatcherRule
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.ObserveDishUsageUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.PickRandomDishUseCase
import com.gabrieltagama.menuplanner.feature.calendar.editor.PlanEditorEvent
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * DinnerEditorViewModel over the real use cases and in-memory repositories: only dinner options are
 * offered (sorted by name), an existing dinner is loaded, saving and clearing persist and close,
 * and the random pick avoids the current dish. Robolectric because the route is read with toRoute;
 * each test keeps a collector on the WhileSubscribed uiState, as the screen does.
 */
@RunWith(AndroidJUnit4::class)
class DinnerEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val date = LocalDate.of(2026, 10, 9)
    private val fixedInstant = Instant.parse("2026-10-09T10:00:00Z")
    private val clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)

    private val tortilla = dish("tortilla", "Tortilla", DishType.SINGLE, MealTime.DINNER)
    private val crema = dish("crema", "Crema de calabacín", DishType.STARTER, MealTime.ANY)
    private val lentejas = dish("lentejas", "Lentejas", DishType.SINGLE, MealTime.LUNCH)
    private val flan = dish("flan", "Flan", DishType.DESSERT, MealTime.ANY)

    private val dishRepository = FakeDishRepository(listOf(tortilla, crema, lentejas, flan))

    @Test
    fun onlyDinnerOptionsAreOfferedSortedByNameAndAnEmptyDayCannotSave() = withViewModel { viewModel, _ ->
        val state = awaitState(viewModel) { !it.status.isLoading && !it.isOptionsLoading }

        assertEquals(listOf(crema, tortilla), state.options)
        assertFalse(state.status.hasSavedPlan)
        assertFalse(state.canSave)
    }

    @Test
    fun savingTheSelectedDishPersistsTheDinnerAndCloses() = withViewModel { viewModel, dinnerPlanRepository ->
        awaitState(viewModel) { !it.status.isLoading && !it.isOptionsLoading }
        viewModel.onDishSelect(tortilla)
        awaitState(viewModel) { it.canSave }

        viewModel.onSave()

        assertEquals(PlanEditorEvent.Close, withTimeout(TIMEOUT_MS) { viewModel.events.first() })
        assertEquals(DinnerDay(date, tortilla, fixedInstant), dinnerPlanRepository.saved.single())
    }

    @Test
    fun existingDinnerIsLoaded() = withViewModel(initial = listOf(DinnerDay(date, crema, Instant.EPOCH))) { viewModel, _ ->
        val state = awaitState(viewModel) { !it.status.isLoading && !it.isOptionsLoading }

        assertTrue(state.status.hasSavedPlan)
        assertEquals(crema, state.form.selected)
        assertTrue(state.canSave)
    }

    @Test
    fun randomPickAvoidsTheCurrentDish() = withViewModel { viewModel, _ ->
        awaitState(viewModel) { !it.status.isLoading && !it.isOptionsLoading }
        viewModel.onDishSelect(tortilla)
        awaitState(viewModel) { it.form.selected == tortilla }

        viewModel.onRandomDish()

        val state = awaitState(viewModel) { it.form.selected != tortilla }
        assertNotEquals(tortilla, state.form.selected)
        assertEquals(crema, state.form.selected)
    }

    @Test
    fun clearingTheDinnerRemovesItAndCloses() = withViewModel(initial = listOf(DinnerDay(date, crema, Instant.EPOCH))) { viewModel, dinnerPlanRepository ->
        awaitState(viewModel) { it.status.hasSavedPlan }
        viewModel.onClearRequest()
        awaitState(viewModel) { it.status.showClearConfirm }

        viewModel.onClearConfirm()

        assertEquals(PlanEditorEvent.Close, withTimeout(TIMEOUT_MS) { viewModel.events.first() })
        assertEquals(listOf(date), dinnerPlanRepository.cleared)
        assertTrue(dinnerPlanRepository.current.isEmpty())
    }

    private fun withViewModel(
        initial: List<DinnerDay> = emptyList(),
        block: suspend CoroutineScope.(DinnerEditorViewModel, FakeDinnerPlanRepository) -> Unit
    ) = runBlocking {
        val dinnerPlanRepository = FakeDinnerPlanRepository(initial)
        val viewModel = DinnerEditorViewModel(
            savedStateHandle = SavedStateHandle(mapOf("epochDay" to date.toEpochDay())),
            dishCatalog = dishRepository,
            observeDishUsage = ObserveDishUsageUseCase(FakeDishUsageQueries(), clock),
            commands = TestPlanCommands(dinners = dinnerPlanRepository, clock = clock).dinner,
            pickRandomDish = PickRandomDishUseCase(Random(7)),
            defaultDispatcher = Dispatchers.Default
        )
        val collector: Job = launch { viewModel.uiState.collect {} }
        try {
            block(viewModel, dinnerPlanRepository)
        } finally {
            collector.cancel()
        }
    }

    private suspend fun awaitState(
        viewModel: DinnerEditorViewModel,
        predicate: (DinnerEditorUiState) -> Boolean
    ): DinnerEditorUiState = withTimeout(TIMEOUT_MS) { viewModel.uiState.first(predicate) }

    private fun dish(id: String, name: String, type: DishType, mealTime: MealTime) =
        Dish(id = DishId(id), name = name, type = type, heaviness = Heaviness.MEDIUM, mealTime = mealTime, updatedAt = Instant.EPOCH)

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
