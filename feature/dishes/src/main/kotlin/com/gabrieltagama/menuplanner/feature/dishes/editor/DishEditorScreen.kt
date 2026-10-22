package com.gabrieltagama.menuplanner.feature.dishes.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.Rating
import com.gabrieltagama.menuplanner.core.ui.component.ConfirmDialog
import com.gabrieltagama.menuplanner.core.ui.component.LoadingIndicator
import com.gabrieltagama.menuplanner.core.ui.component.SnackbarMessageEffect
import com.gabrieltagama.menuplanner.core.ui.text.label
import com.gabrieltagama.menuplanner.core.ui.text.message
import com.gabrieltagama.menuplanner.core.ui.theme.MenuPlannerTheme
import com.gabrieltagama.menuplanner.feature.dishes.R

/**
 * Dish create/edit form: stateful route bound to [DishEditorViewModel] (events, snackbar) and a
 * stateless screen receiving the form state and one callback per user action. The form is split
 * into section cards (basics, classification, ingredients, preparation); type is chosen with
 * icon chips, heaviness with a color-coded segmented button, and rating and meal time with
 * dropdowns. The back arrow and the system back ask the ViewModel first, which offers to discard
 * unsaved changes.
 */
@Composable
fun DishEditorScreenRoute(
    onBack: () -> Unit,
    viewModel: DishEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                DishEditorEvent.Close -> currentOnBack()
            }
        }
    }
    SnackbarMessageEffect(message = state.error?.message(), snackbarHostState = snackbarHostState, onShown = viewModel::onErrorShown)

    BackHandler(onBack = viewModel::onBackRequest)

    DishEditorScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        actions = DishEditorActions(
            onBack = viewModel::onBackRequest,
            onSave = viewModel::onSave,
            onDeleteRequest = viewModel::onDeleteRequest,
            onDeleteConfirm = viewModel::onDeleteConfirm,
            onDeleteDismiss = viewModel::onDeleteDismiss,
            onDiscardConfirm = viewModel::onDiscardConfirm,
            onDiscardDismiss = viewModel::onDiscardDismiss,
            onNameChange = viewModel::onNameChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onTypeChange = viewModel::onTypeChange,
            onHeavinessChange = viewModel::onHeavinessChange,
            onRatingChange = viewModel::onRatingChange,
            onMealTimeChange = viewModel::onMealTimeChange,
            onPreparationChange = viewModel::onPreparationChange,
            onAddIngredient = viewModel::onAddIngredient,
            onRemoveIngredient = viewModel::onRemoveIngredient,
            onIngredientNameChange = viewModel::onIngredientNameChange,
            onIngredientQuantityChange = viewModel::onIngredientQuantityChange,
            onIngredientUnitChange = viewModel::onIngredientUnitChange
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishEditorScreen(
    state: DishEditorUiState,
    actions: DishEditorActions,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (state.isEditMode) R.string.dish_editor_title_edit else R.string.dish_editor_title_new))
                },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.dish_editor_back))
                    }
                },
                actions = {
                    if (state.isEditMode)
                        IconButton(onClick = actions.onDeleteRequest, enabled = state.canSubmit) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.dish_editor_delete))
                        }
                    TextButton(onClick = actions.onSave, enabled = state.canSubmit) {
                        Text(stringResource(R.string.dish_editor_save))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val contentModifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
        if (state.isLoading) LoadingIndicator(modifier = contentModifier)
        else DishForm(state = state, actions = actions, modifier = contentModifier)
    }

    if (state.showDeleteConfirm)
        ConfirmDialog(
            title = stringResource(R.string.dish_editor_delete_title),
            text = stringResource(R.string.dish_editor_delete_text, state.form.name),
            onConfirm = actions.onDeleteConfirm,
            onDismiss = actions.onDeleteDismiss,
            confirmLabel = stringResource(R.string.dish_editor_delete)
        )

    if (state.showDiscardConfirm)
        ConfirmDialog(
            title = stringResource(R.string.dish_editor_discard_title),
            text = stringResource(R.string.dish_editor_discard_text),
            onConfirm = actions.onDiscardConfirm,
            onDismiss = actions.onDiscardDismiss,
            confirmLabel = stringResource(R.string.dish_editor_discard)
        )
}

@Composable
private fun DishForm(state: DishEditorUiState, actions: DishEditorActions, modifier: Modifier = Modifier) = Column(
    modifier = modifier.imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
) {
    SectionCard(title = stringResource(R.string.dish_editor_section_basics)) {
        OutlinedTextField(
            value = state.form.name,
            onValueChange = actions.onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.dish_editor_name)) },
            isError = state.isNameInvalid,
            supportingText = { if (state.isNameInvalid) Text(stringResource(R.string.dish_editor_name_required)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
        OutlinedTextField(
            value = state.form.description,
            onValueChange = actions.onDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.dish_editor_description)) },
            minLines = 2,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
    }
    SectionCard(title = stringResource(R.string.dish_editor_section_classification)) {
        TypeSelector(selected = state.form.type, onSelect = actions.onTypeChange)
        HeavinessSelector(selected = state.form.heaviness, onSelect = actions.onHeavinessChange)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EnumDropdown(
                label = stringResource(R.string.dish_editor_rating),
                options = Rating.entries,
                selected = state.form.rating,
                optionLabel = { it.label() },
                onSelect = actions.onRatingChange,
                modifier = Modifier.weight(1f)
            )
            EnumDropdown(
                label = stringResource(R.string.dish_editor_meal_time),
                options = MealTime.entries,
                selected = state.form.mealTime,
                optionLabel = { it.label() },
                onSelect = actions.onMealTimeChange,
                modifier = Modifier.weight(1f)
            )
        }
    }
    SectionCard(title = stringResource(R.string.dish_editor_ingredients)) {
        IngredientsSection(state = state, actions = actions)
    }
    SectionCard(title = stringResource(R.string.dish_editor_preparation)) {
        OutlinedTextField(
            value = state.form.preparation,
            onValueChange = actions.onPreparationChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.dish_editor_preparation)) },
            minLines = 4,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DishEditorScreenPreview() = MenuPlannerTheme {
    DishEditorScreen(
        state = DishEditorUiState(
            isEditMode = true,
            form = DishForm(
                name = "Tortilla de patatas",
                description = "Jugosa, con cebolla",
                type = DishType.MAIN,
                heaviness = Heaviness.MEDIUM,
                rating = Rating.FOUR,
                mealTime = MealTime.LUNCH,
                ingredients = listOf(
                    IngredientForm(key = 0, name = "Patata", quantityText = "500", unit = MeasureUnit.GRAM),
                    IngredientForm(key = 1, name = "Huevo", quantityText = "6", unit = MeasureUnit.UNIT)
                ),
                preparation = "Pelar y freír las patatas..."
            )
        ),
        actions = DishEditorActions()
    )
}
