package com.gabrieltagama.menuplanner.feature.dishes.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.fold
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.Rating
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.DeleteDishUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.GetDishUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.SaveDishUseCase
import com.gabrieltagama.menuplanner.core.ui.state.catchingStorageFailure
import com.gabrieltagama.menuplanner.feature.dishes.navigation.DishEditorRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Create/edit flow of a dish over one DishForm: in edit mode it is filled from the loaded dish and
 * saved with its id, and for a new dish SaveDishUseCase assigns the id. DishDraftStore keeps every
 * change and the form as it was opened, so after the system kills the app the editor comes back
 * with what the user had typed instead of reloading the dish, and going back with unsaved changes
 * still asks before discarding them.
 */
@HiltViewModel
class DishEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getDish: GetDishUseCase,
    private val saveDish: SaveDishUseCase,
    private val deleteDish: DeleteDishUseCase
) : ViewModel() {

    private val dishId: DishId? = savedStateHandle.toRoute<DishEditorRoute>().dishId?.let(::DishId)
    private val drafts = DishDraftStore(savedStateHandle)
    private var openedForm: DishForm? = null

    private val _uiState = MutableStateFlow(DishEditorUiState(isEditMode = dishId != null, isLoading = dishId != null))
    val uiState: StateFlow<DishEditorUiState> = _uiState.asStateFlow()

    private val _events = Channel<DishEditorEvent>(Channel.BUFFERED)
    val events: Flow<DishEditorEvent> = _events.receiveAsFlow()

    init {
        val restored = drafts.current
        when {
            restored != null -> open(restored, opened = drafts.opened ?: restored)
            dishId != null -> load(dishId)
            else -> open(DishForm(), opened = DishForm())
        }
        viewModelScope.launch {
            _uiState.filterNot { it.isLoading }.map { it.form }.distinctUntilChanged().collect { drafts.current = it }
        }
    }

    fun onNameChange(value: String) = updateForm { it.copy(name = value) }

    fun onDescriptionChange(value: String) = updateForm { it.copy(description = value) }

    fun onTypeChange(value: DishType) = updateForm { it.copy(type = value) }

    fun onHeavinessChange(value: Heaviness) = updateForm { it.copy(heaviness = value) }

    fun onRatingChange(value: Rating) = updateForm { it.copy(rating = value) }

    fun onMealTimeChange(value: MealTime) = updateForm { it.copy(mealTime = value) }

    fun onPreparationChange(value: String) = updateForm { it.copy(preparation = value) }

    fun onAddIngredient() = updateForm { it.copy(ingredients = it.ingredients + IngredientForm(key = it.nextIngredientKey)) }

    fun onRemoveIngredient(key: Long) = updateForm { form -> form.copy(ingredients = form.ingredients.filterNot { it.key == key }) }

    fun onIngredientNameChange(key: Long, value: String) = updateIngredient(key) { it.copy(name = value) }

    fun onIngredientQuantityChange(key: Long, value: String) = updateIngredient(key) { it.copy(quantityText = value) }

    fun onIngredientUnitChange(key: Long, value: MeasureUnit) = updateIngredient(key) { it.copy(unit = value) }

    fun onDeleteRequest() = _uiState.update { it.copy(showDeleteConfirm = true) }

    fun onDeleteDismiss() = _uiState.update { it.copy(showDeleteConfirm = false) }

    fun onErrorShown() = _uiState.update { it.copy(error = null) }

    fun onBackRequest() {
        if (hasUnsavedChanges()) _uiState.update { it.copy(showDiscardConfirm = true) }
        else close()
    }

    fun onDiscardConfirm() {
        _uiState.update { it.copy(showDiscardConfirm = false) }
        close()
    }

    fun onDiscardDismiss() = _uiState.update { it.copy(showDiscardConfirm = false) }

    fun onSave() {
        val state = _uiState.value
        if (!state.canSubmit) return
        val draft = state.form.toDraft(dishId)?.takeIf { it.validationError == null }
        if (draft == null) {
            _uiState.update { it.copy(showValidationErrors = true) }
            return
        }
        _uiState.update { it.copy(isSaving = true, showValidationErrors = true) }
        viewModelScope.launch { catchingStorageFailure { saveDish(draft) }.fold(onSuccess = { _events.send(DishEditorEvent.Close) }, onFailure = ::showError) }
    }

    fun onDeleteConfirm() {
        val id = dishId ?: return
        _uiState.update { it.copy(showDeleteConfirm = false, isSaving = true) }
        viewModelScope.launch { catchingStorageFailure { deleteDish(id) }.fold(onSuccess = { _events.send(DishEditorEvent.Close) }, onFailure = ::showError) }
    }

    private fun load(id: DishId) = viewModelScope.launch {
        catchingStorageFailure { getDish(id) }.fold(
            onSuccess = { dish -> DishForm.of(dish).let { open(it, opened = it) } },
            onFailure = { error -> _uiState.update { it.copy(isLoading = false, error = error) } }
        )
    }

    private fun open(form: DishForm, opened: DishForm) {
        openedForm = opened
        drafts.opened = opened
        _uiState.update { it.copy(isLoading = false, form = form) }
    }

    private fun showError(error: DomainError) = _uiState.update { it.copy(isSaving = false, error = error) }

    private fun hasUnsavedChanges(): Boolean = openedForm?.let { it != _uiState.value.form } ?: false

    private fun close() {
        viewModelScope.launch { _events.send(DishEditorEvent.Close) }
    }

    private fun updateForm(transform: (DishForm) -> DishForm) = _uiState.update { it.copy(form = transform(it.form)) }

    private fun updateIngredient(key: Long, transform: (IngredientForm) -> IngredientForm) =
        updateForm { form -> form.copy(ingredients = form.ingredients.map { if (it.key == key) transform(it) else it }) }
}
