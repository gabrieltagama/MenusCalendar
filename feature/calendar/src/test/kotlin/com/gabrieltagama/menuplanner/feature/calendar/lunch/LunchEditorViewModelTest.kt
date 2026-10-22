package com.gabrieltagama.menuplanner.feature.calendar.lunch

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishUsageQueries
import com.gabrieltagama.menuplanner.core.domain.fake.FakeLunchPlanRepository
import com.gabrieltagama.menuplanner.core.domain.fake.TestPlanCommands
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.LunchDay
import com.gabrieltagama.menuplanner.core.domain.model.MenuShape
import com.gabrieltagama.menuplanner.core.domain.model.MenuSlot
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * LunchEditorViewModel over the real use cases and in-memory repositories.
 *
 * Instrumented because the ViewModel reads savedStateHandle.toRoute<LunchEditorRoute>() (Bundle based
 * decoding). The handle uses the route property name "epochDay". uiState is a WhileSubscribed
 * StateFlow and onSave reads uiState.value, so each test keeps a collector active, as the screen does.
 */
@RunWith(AndroidJUnit4::class)
class LunchEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val date = LocalDate.of(2026, 10, 9)
    private val fixedInstant = Instant.parse("2026-10-09T10:00:00Z")
    private val clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)

    private val starter = dish("starter", "Gazpacho", DishType.STARTER)
    private val main = dish("main", "Pollo asado", DishType.MAIN)
    private val single = dish("single", "Lentejas", DishType.SINGLE)
    private val dessert = dish("dessert", "Flan", DishType.DESSERT)

    private val dishRepository = FakeDishRepository(listOf(starter, main, single, dessert))

    @Test
    fun emptyDayStartsInCoursesModeAndCannotSave() = withViewModel { viewModel, _ ->
        val state = awaitState(viewModel) { !it.status.isLoading && !it.isOptionsLoading }

        assertEquals(MenuShape.COURSES, state.form.mode)
        assertFalse(state.status.hasSavedPlan)
        assertFalse(state.canSave)
        assertEquals(listOf(MenuSlot.STARTER, MenuSlot.MAIN), state.visibleSlots)
        assertEquals(listOf(starter), state.optionsFor(MenuSlot.STARTER))
        assertFalse(state.options.containsKey(DishType.DESSERT))
    }

    @Test
    fun requiredSlotsEnableSave() = withViewModel { viewModel, _ ->
        awaitState(viewModel) { !it.status.isLoading && !it.isOptionsLoading }

        viewModel.onDishSelect(MenuSlot.STARTER, starter)
        assertFalse(awaitState(viewModel) { it.selectionFor(MenuSlot.STARTER) == starter }.canSave)

        viewModel.onDishSelect(MenuSlot.MAIN, main)
        assertTrue(awaitState(viewModel) { it.selectionFor(MenuSlot.MAIN) == main }.canSave)

        viewModel.onDishSelect(MenuSlot.MAIN, null)
        assertFalse(awaitState(viewModel) { it.selectionFor(MenuSlot.MAIN) == null }.canSave)
    }

    @Test
    fun modeSwitchKeepsSelectionsAndChangesRequiredSlots() = withViewModel { viewModel, _ ->
        awaitState(viewModel) { !it.status.isLoading && !it.isOptionsLoading }
        viewModel.onDishSelect(MenuSlot.STARTER, starter)
        viewModel.onDishSelect(MenuSlot.MAIN, main)
        awaitState(viewModel) { it.canSave }

        viewModel.onModeChange(MenuShape.SINGLE)

        val singleState = awaitState(viewModel) { it.form.mode == MenuShape.SINGLE }
        assertEquals(listOf(MenuSlot.SINGLE), singleState.visibleSlots)
        assertFalse(singleState.canSave)

        viewModel.onDishSelect(MenuSlot.SINGLE, single)
        assertTrue(awaitState(viewModel) { it.selectionFor(MenuSlot.SINGLE) == single }.canSave)

        viewModel.onModeChange(MenuShape.COURSES)
        val coursesState = awaitState(viewModel) { it.form.mode == MenuShape.COURSES }
        assertEquals(starter, coursesState.selectionFor(MenuSlot.STARTER))
        assertEquals(main, coursesState.selectionFor(MenuSlot.MAIN))
    }

    @Test
    fun saveCallsRepositoryWithActiveModeOnlyAndCloses() = withViewModel { viewModel, lunchPlanRepository ->
        awaitState(viewModel) { !it.status.isLoading && !it.isOptionsLoading }
        viewModel.onDishSelect(MenuSlot.STARTER, starter)
        viewModel.onDishSelect(MenuSlot.SINGLE, single)
        viewModel.onModeChange(MenuShape.SINGLE)
        awaitState(viewModel) { it.form.mode == MenuShape.SINGLE && it.canSave }

        viewModel.onSave()

        assertEquals(PlanEditorEvent.Close, withTimeout(TIMEOUT_MS) { viewModel.events.first() })
        val saved = lunchPlanRepository.saved.single()
        assertEquals(date, saved.date)
        assertEquals(DailyMenu.Single(single = single), saved.menu)
        assertEquals(fixedInstant, saved.updatedAt)
    }

    @Test
    fun existingSingleDayIsLoadedInSingleMode() {
        val existing = LunchDay(date = date, menu = DailyMenu.Single(single = single), updatedAt = Instant.EPOCH)
        withViewModel(initialDays = listOf(existing)) { viewModel, _ ->
            val state = awaitState(viewModel) { !it.status.isLoading && !it.isOptionsLoading }

            assertTrue(state.status.hasSavedPlan)
            assertEquals(MenuShape.SINGLE, state.form.mode)
            assertEquals(single, state.selectionFor(MenuSlot.SINGLE))
            assertTrue(state.canSave)
        }
    }

    @Test
    fun clearDayCallsRepositoryAndCloses() {
        val existing = LunchDay(date = date, menu = DailyMenu.Courses(starter = starter, main = main), updatedAt = Instant.EPOCH)
        withViewModel(initialDays = listOf(existing)) { viewModel, lunchPlanRepository ->
            awaitState(viewModel) { it.status.hasSavedPlan }
            viewModel.onClearRequest()
            assertTrue(awaitState(viewModel) { it.status.showClearConfirm }.status.showClearConfirm)

            viewModel.onClearConfirm()

            assertEquals(PlanEditorEvent.Close, withTimeout(TIMEOUT_MS) { viewModel.events.first() })
            assertEquals(listOf(date), lunchPlanRepository.cleared)
            assertTrue(lunchPlanRepository.current.isEmpty())
        }
    }

    @Test
    fun clearDismissHidesConfirmation() = withViewModel { viewModel, lunchPlanRepository ->
        awaitState(viewModel) { !it.status.isLoading }
        viewModel.onClearRequest()
        awaitState(viewModel) { it.status.showClearConfirm }

        viewModel.onClearDismiss()

        assertFalse(awaitState(viewModel) { !it.status.showClearConfirm }.status.showClearConfirm)
        assertTrue(lunchPlanRepository.cleared.isEmpty())
    }

    private fun withViewModel(
        initialDays: List<LunchDay> = emptyList(),
        block: suspend CoroutineScope.(LunchEditorViewModel, FakeLunchPlanRepository) -> Unit
    ) = runBlocking {
        val lunchPlanRepository = FakeLunchPlanRepository(initialDays)
        val viewModel = LunchEditorViewModel(
            savedStateHandle = SavedStateHandle(mapOf("epochDay" to date.toEpochDay())),
            dishCatalog = dishRepository,
            observeDishUsage = ObserveDishUsageUseCase(FakeDishUsageQueries(), clock),
            commands = TestPlanCommands(lunches = lunchPlanRepository, clock = clock).lunch,
            pickRandomDish = PickRandomDishUseCase(Random(7)),
            defaultDispatcher = Dispatchers.Default
        )
        val collector: Job = launch { viewModel.uiState.collect {} }
        try {
            block(viewModel, lunchPlanRepository)
        } finally {
            collector.cancel()
        }
    }

    private suspend fun awaitState(
        viewModel: LunchEditorViewModel,
        predicate: (LunchEditorUiState) -> Boolean
    ): LunchEditorUiState = withTimeout(TIMEOUT_MS) { viewModel.uiState.first(predicate) }

    private fun dish(id: String, name: String, type: DishType) =
        Dish(id = DishId(id), name = name, type = type, heaviness = Heaviness.MEDIUM, updatedAt = Instant.EPOCH)

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
