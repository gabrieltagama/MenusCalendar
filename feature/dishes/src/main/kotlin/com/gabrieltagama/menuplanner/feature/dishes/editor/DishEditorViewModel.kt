package com.gabrieltagama.menuplanner.feature.dishes.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishDraft
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.Rating
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.DeleteDishUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.GetDishUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.SaveDishUseCase
import com.gabrieltagama.menuplanner.feature.dishes.navigation.DishEditorRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Create/edit form of a dish. The form is saved as a DishDraft: in edit mode it carries the id of
 * the loaded dish, and for a new dish SaveDishUseCase assigns the id.
 */
sealed interface DishEditorEvent {
    data object Close : DishEditorEvent
}

@HiltViewModel
class DishEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getDish: GetDishUseCase,
    private val saveDish: SaveDishUseCase,
    private val deleteDish: DeleteDishUseCase
) : ViewModel() {

    private val dishId: DishId? = savedStateHandle.toRoute<DishEditorRoute>().dishId?.let(::DishId)
    private var nextIngredientKey = 0L

    private val _uiState = MutableStateFlow(DishEditorUiState(isEditMode = dishId != null, isLoading = dishId != null))
    val uiState: StateFlow<DishEditorUiState> = _uiState.asStateFlow()

    private val _events = Channel<DishEditorEvent>(Channel.BUFFERED)
    val events: Flow<DishEditorEvent> = _events.receiveAsFlow()

    init {
        dishId?.let(::load)
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }

    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }

    fun onTypeChange(value: DishType) = _uiState.update { it.copy(type = value) }

    fun onHeavinessChange(value: Heaviness) = _uiState.update { it.copy(heaviness = value) }

    fun onRatingChange(value: Rating) = _uiState.update { it.copy(rating = value) }

    fun onMealTimeChange(value: MealTime) = _uiState.update { it.copy(mealTime = value) }

    fun onPreparationChange(value: String) = _uiState.update { it.copy(preparation = value) }

    fun onAddIngredient() = _uiState.update { it.copy(ingredients = it.ingredients + IngredientForm(key = newKey())) }

    fun onRemoveIngredient(key: Long) = _uiState.update { state ->
        state.copy(ingredients = state.ingredients.filterNot { it.key == key })
    }

    fun onIngredientNameChange(key: Long, value: String) = updateIngredient(key) { it.copy(name = value) }

    fun onIngredientQuantityChange(key: Long, value: String) = updateIngredient(key) { it.copy(quantityText = value) }

    fun onIngredientUnitChange(key: Long, value: MeasureUnit) = updateIngredient(key) { it.copy(unit = value) }

    fun onDeleteRequest() = _uiState.update { it.copy(showDeleteConfirm = true) }

    fun onDeleteDismiss() = _uiState.update { it.copy(showDeleteConfirm = false) }

    fun onErrorShown() = _uiState.update { it.copy(error = null) }

    fun onSave() {
        val state = _uiState.value
        if (!state.canSubmit) return
        val isFormValid = state.name.isNotBlank() && state.ingredients.all { it.isNameValid && it.isQuantityValid }
        if (!isFormValid) {
            _uiState.update { it.copy(showValidationErrors = true) }
            return
        }
        val draft = draftOf(state)
        _uiState.update { it.copy(isSaving = true, showValidationErrors = true) }
        viewModelScope.launch {
            when (val outcome = saveDish(draft)) {
                is Outcome.Success -> _events.send(DishEditorEvent.Close)
                is Outcome.Failure -> _uiState.update { it.copy(isSaving = false, error = outcome.error) }
            }
        }
    }

    fun onDeleteConfirm() {
        val id = dishId ?: return
        _uiState.update { it.copy(showDeleteConfirm = false, isSaving = true) }
        viewModelScope.launch {
            when (val outcome = deleteDish(id)) {
                is Outcome.Success -> _events.send(DishEditorEvent.Close)
                is Outcome.Failure -> _uiState.update { it.copy(isSaving = false, error = outcome.error) }
            }
        }
    }

    private fun load(id: DishId) = viewModelScope.launch {
        when (val outcome = getDish(id)) {
            is Outcome.Success -> fillForm(outcome.value)
            is Outcome.Failure -> _uiState.update { it.copy(isLoading = false, error = outcome.error) }
        }
    }

    private fun fillForm(dish: Dish) {
        _uiState.update {
            it.copy(
                isLoading = false,
                name = dish.name,
                description = dish.description,
                type = dish.type,
                heaviness = dish.heaviness,
                rating = dish.rating,
                mealTime = dish.mealTime,
                ingredients = dish.ingredients.map { ingredient -> ingredient.toForm(newKey()) },
                preparation = dish.preparation
            )
        }
    }

    private fun draftOf(state: DishEditorUiState): DishDraft = DishDraft(
        id = dishId,
        name = state.name,
        description = state.description,
        ingredients = state.ingredients.mapNotNull { it.toDomain() },
        preparation = state.preparation,
        type = state.type,
        heaviness = state.heaviness,
        rating = state.rating,
        mealTime = state.mealTime
    )

    private fun updateIngredient(key: Long, transform: (IngredientForm) -> IngredientForm) = _uiState.update { state ->
        state.copy(ingredients = state.ingredients.map { if (it.key == key) transform(it) else it })
    }

    private fun newKey(): Long = nextIngredientKey++
}
