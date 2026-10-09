package com.gabrieltagama.menuplanner.feature.dishes.editor

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.DeleteDishUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.GetDishUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.SaveDishUseCase
import com.gabrieltagama.menuplanner.feature.dishes.testing.FakeDishRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * DishEditorViewModel over the real dish use cases and an in-memory repository.
 *
 * Instrumented on purpose: the ViewModel reads its argument with savedStateHandle.toRoute<DishEditorRoute>(),
 * which decodes through NavType and android.os.Bundle, so it cannot run as a plain JVM test without
 * Robolectric. The handle is built with the route property name as key ("dishId"), exactly as the
 * navigation runtime stores it; an empty handle decodes to the default DishEditorRoute(dishId = null).
 * viewModelScope runs on the real main looper, so state is awaited with first { } under a timeout.
 */
@RunWith(AndroidJUnit4::class)
class DishEditorViewModelTest {

    private val fixedInstant: Instant = Instant.parse("2026-10-09T10:00:00Z")
    private val clock: Clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)

    private val existingDish = Dish(
        id = "tortilla",
        name = "Tortilla de patatas",
        description = "Jugosa",
        ingredients = listOf(
            Ingredient(name = "Patata", quantity = 500.0, unit = MeasureUnit.GRAM),
            Ingredient(name = "Aceite", quantity = 0.25, unit = MeasureUnit.LITER)
        ),
        preparation = "Freír",
        type = DishType.MAIN,
        heaviness = Heaviness.VERY_HIGH,
        updatedAt = Instant.parse("2020-01-01T00:00:00Z")
    )

    private val repository = FakeDishRepository(listOf(existingDish))

    @Test
    fun newDishStartsEmptyAndNotInEditMode() {
        val viewModel = createViewModel(dishId = null)

        val state = viewModel.uiState.value
        assertFalse(state.isEditMode)
        assertFalse(state.isLoading)
        assertTrue(state.canSubmit)
        assertEquals("", state.name)
    }

    @Test
    fun blankNameShowsValidationErrorAndDoesNotSave() {
        val viewModel = createViewModel(dishId = null)
        viewModel.onNameChange("   ")

        viewModel.onSave()

        val state = viewModel.uiState.value
        assertTrue(state.showValidationErrors)
        assertTrue(state.isNameInvalid)
        assertFalse(state.isSaving)
        assertTrue(repository.upserted.isEmpty())
    }

    @Test
    fun invalidQuantityTextShowsValidationErrorAndDoesNotSave() {
        val viewModel = createViewModel(dishId = null)
        viewModel.onNameChange("Gazpacho")
        viewModel.onAddIngredient()
        val key = viewModel.uiState.value.ingredients.single().key
        viewModel.onIngredientNameChange(key, "Tomate")
        viewModel.onIngredientQuantityChange(key, "1,2,3")

        viewModel.onSave()

        val state = viewModel.uiState.value
        assertTrue(state.showValidationErrors)
        assertFalse(state.isNameInvalid)
        assertFalse(state.ingredients.single().isQuantityValid)
        assertTrue(repository.upserted.isEmpty())
    }

    @Test
    fun blankQuantityIsAcceptedOnlyForToTaste() {
        val ingredient = IngredientForm(key = 0, name = "Sal", quantityText = "")

        assertFalse(ingredient.isQuantityValid)
        assertEquals(0.0, ingredient.copy(unit = MeasureUnit.TO_TASTE).parsedQuantity)
    }

    @Test
    fun successfulSaveEmitsCloseAndPersistsNormalizedDish() = runBlocking {
        val viewModel = createViewModel(dishId = null)
        viewModel.onNameChange("  Gazpacho  ")
        viewModel.onTypeChange(DishType.STARTER)
        viewModel.onHeavinessChange(Heaviness.VERY_LOW)
        viewModel.onAddIngredient()
        val key = viewModel.uiState.value.ingredients.single().key
        viewModel.onIngredientNameChange(key, "Tomate")
        viewModel.onIngredientQuantityChange(key, "1,5")
        viewModel.onIngredientUnitChange(key, MeasureUnit.KILOGRAM)

        viewModel.onSave()

        assertEquals(DishEditorEvent.Close, withTimeout(TIMEOUT_MS) { viewModel.events.first() })
        val saved = repository.upserted.single()
        assertEquals("Gazpacho", saved.name)
        assertEquals(DishType.STARTER, saved.type)
        assertEquals(Heaviness.VERY_LOW, saved.heaviness)
        assertEquals(listOf(Ingredient(name = "Tomate", quantity = 1.5, unit = MeasureUnit.KILOGRAM)), saved.ingredients)
        assertEquals(fixedInstant, saved.updatedAt)
    }

    @Test
    fun editModeLoadsDish() = runBlocking {
        val viewModel = createViewModel(dishId = existingDish.id)

        val state = awaitLoaded(viewModel)

        assertTrue(state.isEditMode)
        assertEquals(existingDish.name, state.name)
        assertEquals(existingDish.description, state.description)
        assertEquals(existingDish.type, state.type)
        assertEquals(existingDish.heaviness, state.heaviness)
        assertEquals(existingDish.preparation, state.preparation)
        assertEquals(listOf("500", "0,25"), state.ingredients.map { it.quantityText })
        assertEquals(listOf("Patata", "Aceite"), state.ingredients.map { it.name })
        assertEquals(state.ingredients.size, state.ingredients.map { it.key }.toSet().size)
    }

    @Test
    fun editModeSaveKeepsDishId() = runBlocking {
        val viewModel = createViewModel(dishId = existingDish.id)
        awaitLoaded(viewModel)
        viewModel.onNameChange("Tortilla con cebolla")

        viewModel.onSave()

        assertEquals(DishEditorEvent.Close, withTimeout(TIMEOUT_MS) { viewModel.events.first() })
        val saved = repository.upserted.single()
        assertEquals(existingDish.id, saved.id)
        assertEquals("Tortilla con cebolla", saved.name)
        assertEquals(existingDish.ingredients, saved.ingredients)
    }

    @Test
    fun unknownDishIdShowsDishNotFound() = runBlocking {
        val viewModel = createViewModel(dishId = "missing")

        val state = awaitLoaded(viewModel)

        assertEquals(DomainError.DishNotFound, state.error)
    }

    @Test
    fun deleteBlockedWhenDishIsUsedInCalendar() = runBlocking {
        repository.usedInCalendar += existingDish.id
        val viewModel = createViewModel(dishId = existingDish.id)
        awaitLoaded(viewModel)
        viewModel.onDeleteRequest()
        assertTrue(viewModel.uiState.value.showDeleteConfirm)

        viewModel.onDeleteConfirm()

        val state = withTimeout(TIMEOUT_MS) { viewModel.uiState.first { it.error != null } }
        assertEquals(DomainError.DishInUse, state.error)
        assertFalse(state.isSaving)
        assertFalse(state.showDeleteConfirm)
        assertTrue(repository.deletedIds.isEmpty())

        viewModel.onErrorShown()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun deleteNotUsedDishEmitsClose() = runBlocking {
        val viewModel = createViewModel(dishId = existingDish.id)
        awaitLoaded(viewModel)

        viewModel.onDeleteRequest()
        viewModel.onDeleteConfirm()

        assertEquals(DishEditorEvent.Close, withTimeout(TIMEOUT_MS) { viewModel.events.first() })
        assertEquals(listOf(existingDish.id), repository.deletedIds)
    }

    private fun createViewModel(dishId: String?): DishEditorViewModel = DishEditorViewModel(
        savedStateHandle = if (dishId == null) SavedStateHandle() else SavedStateHandle(mapOf("dishId" to dishId)),
        getDish = GetDishUseCase(repository),
        saveDish = SaveDishUseCase(repository, clock),
        deleteDish = DeleteDishUseCase(repository)
    )

    private suspend fun awaitLoaded(viewModel: DishEditorViewModel): DishEditorUiState =
        withTimeout(TIMEOUT_MS) { viewModel.uiState.first { !it.isLoading } }

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
