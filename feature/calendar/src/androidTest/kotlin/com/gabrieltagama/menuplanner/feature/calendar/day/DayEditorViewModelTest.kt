package com.gabrieltagama.menuplanner.feature.calendar.day

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gabrieltagama.menuplanner.core.domain.common.MenuSlot
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.ObserveDishesUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.ClearMealDayUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.GetMealDayUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.SaveMealDayUseCase
import com.gabrieltagama.menuplanner.feature.calendar.testing.FakeDishRepository
import com.gabrieltagama.menuplanner.feature.calendar.testing.FakeMealPlanRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * DayEditorViewModel over the real use cases and in-memory repositories.
 *
 * Instrumented because the ViewModel reads savedStateHandle.toRoute<DayEditorRoute>() (Bundle based
 * decoding). The handle uses the route property name "epochDay". uiState is a WhileSubscribed
 * StateFlow and onSave reads uiState.value, so each test keeps a collector active, as the screen does.
 */
@RunWith(AndroidJUnit4::class)
class DayEditorViewModelTest {

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
        val state = awaitState(viewModel) { !it.form.isLoading && !it.isOptionsLoading }

        assertEquals(MenuMode.COURSES, state.form.mode)
        assertFalse(state.form.hasSavedDay)
        assertFalse(state.canSave)
        assertEquals(listOf(MenuSlot.STARTER, MenuSlot.MAIN, MenuSlot.DESSERT), state.visibleSlots)
        assertEquals(listOf(starter), state.optionsFor(MenuSlot.STARTER))
        assertEquals(listOf(dessert), state.optionsFor(MenuSlot.DESSERT))
    }

    @Test
    fun requiredSlotsEnableSave() = withViewModel { viewModel, _ ->
        awaitState(viewModel) { !it.form.isLoading && !it.isOptionsLoading }

        viewModel.onDishSelect(MenuSlot.STARTER, starter)
        assertFalse(awaitState(viewModel) { it.selectionFor(MenuSlot.STARTER) == starter }.canSave)

        viewModel.onDishSelect(MenuSlot.MAIN, main)
        assertTrue(awaitState(viewModel) { it.selectionFor(MenuSlot.MAIN) == main }.canSave)

        viewModel.onDishSelect(MenuSlot.MAIN, null)
        assertFalse(awaitState(viewModel) { it.selectionFor(MenuSlot.MAIN) == null }.canSave)
    }

    @Test
    fun modeSwitchKeepsSelectionsAndChangesRequiredSlots() = withViewModel { viewModel, _ ->
        awaitState(viewModel) { !it.form.isLoading && !it.isOptionsLoading }
        viewModel.onDishSelect(MenuSlot.STARTER, starter)
        viewModel.onDishSelect(MenuSlot.MAIN, main)
        awaitState(viewModel) { it.canSave }

        viewModel.onModeChange(MenuMode.SINGLE)

        val singleState = awaitState(viewModel) { it.form.mode == MenuMode.SINGLE }
        assertEquals(listOf(MenuSlot.SINGLE, MenuSlot.DESSERT), singleState.visibleSlots)
        assertFalse(singleState.canSave)

        viewModel.onDishSelect(MenuSlot.SINGLE, single)
        assertTrue(awaitState(viewModel) { it.selectionFor(MenuSlot.SINGLE) == single }.canSave)

        viewModel.onModeChange(MenuMode.COURSES)
        val coursesState = awaitState(viewModel) { it.form.mode == MenuMode.COURSES }
        assertEquals(starter, coursesState.selectionFor(MenuSlot.STARTER))
        assertEquals(main, coursesState.selectionFor(MenuSlot.MAIN))
    }

    @Test
    fun saveCallsRepositoryWithActiveModeOnlyAndCloses() = withViewModel { viewModel, mealPlanRepository ->
        awaitState(viewModel) { !it.form.isLoading && !it.isOptionsLoading }
        viewModel.onDishSelect(MenuSlot.STARTER, starter)
        viewModel.onDishSelect(MenuSlot.SINGLE, single)
        viewModel.onDishSelect(MenuSlot.DESSERT, dessert)
        viewModel.onModeChange(MenuMode.SINGLE)
        awaitState(viewModel) { it.form.mode == MenuMode.SINGLE && it.canSave }

        viewModel.onSave()

        assertEquals(DayEditorEvent.Close, withTimeout(TIMEOUT_MS) { viewModel.events.first() })
        val saved = mealPlanRepository.saved.single()
        assertEquals(date, saved.date)
        assertEquals(DailyMenu.Single(single = single, dessert = dessert), saved.menu)
        assertEquals(fixedInstant, saved.updatedAt)
    }

    @Test
    fun existingSingleDayIsLoadedInSingleMode() {
        val existing = MealDay(date = date, menu = DailyMenu.Single(single = single, dessert = dessert))
        withViewModel(initialDays = listOf(existing)) { viewModel, _ ->
            val state = awaitState(viewModel) { !it.form.isLoading && !it.isOptionsLoading }

            assertTrue(state.form.hasSavedDay)
            assertEquals(MenuMode.SINGLE, state.form.mode)
            assertEquals(single, state.selectionFor(MenuSlot.SINGLE))
            assertEquals(dessert, state.selectionFor(MenuSlot.DESSERT))
            assertTrue(state.canSave)
        }
    }

    @Test
    fun clearDayCallsRepositoryAndCloses() {
        val existing = MealDay(date = date, menu = DailyMenu.Courses(starter = starter, main = main))
        withViewModel(initialDays = listOf(existing)) { viewModel, mealPlanRepository ->
            awaitState(viewModel) { it.form.hasSavedDay }
            viewModel.onClearRequest()
            assertTrue(awaitState(viewModel) { it.form.showClearConfirm }.form.showClearConfirm)

            viewModel.onClearConfirm()

            assertEquals(DayEditorEvent.Close, withTimeout(TIMEOUT_MS) { viewModel.events.first() })
            assertEquals(listOf(date), mealPlanRepository.cleared)
            assertTrue(mealPlanRepository.current.isEmpty())
        }
    }

    @Test
    fun clearDismissHidesConfirmation() = withViewModel { viewModel, mealPlanRepository ->
        awaitState(viewModel) { !it.form.isLoading }
        viewModel.onClearRequest()
        awaitState(viewModel) { it.form.showClearConfirm }

        viewModel.onClearDismiss()

        assertFalse(awaitState(viewModel) { !it.form.showClearConfirm }.form.showClearConfirm)
        assertTrue(mealPlanRepository.cleared.isEmpty())
    }

    private fun withViewModel(
        initialDays: List<MealDay> = emptyList(),
        block: suspend CoroutineScope.(DayEditorViewModel, FakeMealPlanRepository) -> Unit
    ) = runBlocking {
        val mealPlanRepository = FakeMealPlanRepository(initialDays)
        val viewModel = DayEditorViewModel(
            savedStateHandle = SavedStateHandle(mapOf("epochDay" to date.toEpochDay())),
            observeDishes = ObserveDishesUseCase(dishRepository),
            getMealDay = GetMealDayUseCase(mealPlanRepository),
            saveMealDay = SaveMealDayUseCase(mealPlanRepository, clock),
            clearMealDay = ClearMealDayUseCase(mealPlanRepository)
        )
        val collector: Job = launch { viewModel.uiState.collect {} }
        try {
            block(viewModel, mealPlanRepository)
        } finally {
            collector.cancel()
        }
    }

    private suspend fun awaitState(
        viewModel: DayEditorViewModel,
        predicate: (DayEditorUiState) -> Boolean
    ): DayEditorUiState = withTimeout(TIMEOUT_MS) { viewModel.uiState.first(predicate) }

    private fun dish(id: String, name: String, type: DishType) =
        Dish(id = id, name = name, type = type, heaviness = Heaviness.MEDIUM)

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
