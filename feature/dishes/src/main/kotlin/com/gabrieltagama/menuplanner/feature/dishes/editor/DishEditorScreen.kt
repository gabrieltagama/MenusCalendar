package com.gabrieltagama.menuplanner.feature.dishes.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.ui.component.ConfirmDialog
import com.gabrieltagama.menuplanner.core.ui.component.HeavinessDot
import com.gabrieltagama.menuplanner.core.ui.component.icon
import com.gabrieltagama.menuplanner.core.ui.component.LoadingIndicator
import com.gabrieltagama.menuplanner.core.ui.text.label
import com.gabrieltagama.menuplanner.core.ui.text.message
import com.gabrieltagama.menuplanner.core.ui.theme.MenuPlannerTheme
import com.gabrieltagama.menuplanner.feature.dishes.R

/**
 * Dish create/edit form: stateful route bound to [DishEditorViewModel] (events, snackbar) and a
 * stateless screen receiving the form state and one callback per user action. The form is split
 * into section cards (basics, classification, ingredients, preparation); type is chosen with
 * icon chips and heaviness with a color-coded segmented button.
 */
data class DishEditorActions(
    val onBack: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onDeleteRequest: () -> Unit = {},
    val onDeleteConfirm: () -> Unit = {},
    val onDeleteDismiss: () -> Unit = {},
    val onNameChange: (String) -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val onTypeChange: (DishType) -> Unit = {},
    val onHeavinessChange: (Heaviness) -> Unit = {},
    val onPreparationChange: (String) -> Unit = {},
    val onAddIngredient: () -> Unit = {},
    val onRemoveIngredient: (Long) -> Unit = {},
    val onIngredientNameChange: (Long, String) -> Unit = { _, _ -> },
    val onIngredientQuantityChange: (Long, String) -> Unit = { _, _ -> },
    val onIngredientUnitChange: (Long, MeasureUnit) -> Unit = { _, _ -> }
)

@Composable
fun DishEditorScreenRoute(
    onBack: () -> Unit,
    viewModel: DishEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = state.error?.message()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                DishEditorEvent.Close -> currentOnBack()
            }
        }
    }
    LaunchedEffect(errorMessage) {
        val message = errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.onErrorShown()
    }

    DishEditorScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        actions = DishEditorActions(
            onBack = onBack,
            onSave = viewModel::onSave,
            onDeleteRequest = viewModel::onDeleteRequest,
            onDeleteConfirm = viewModel::onDeleteConfirm,
            onDeleteDismiss = viewModel::onDeleteDismiss,
            onNameChange = viewModel::onNameChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onTypeChange = viewModel::onTypeChange,
            onHeavinessChange = viewModel::onHeavinessChange,
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
            text = stringResource(R.string.dish_editor_delete_text, state.name),
            onConfirm = actions.onDeleteConfirm,
            onDismiss = actions.onDeleteDismiss,
            confirmLabel = stringResource(R.string.dish_editor_delete)
        )
}

@Composable
private fun DishForm(state: DishEditorUiState, actions: DishEditorActions, modifier: Modifier = Modifier) = Column(
    modifier = modifier.imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
) {
    SectionCard(title = stringResource(R.string.dish_editor_section_basics)) {
        OutlinedTextField(
            value = state.name,
            onValueChange = actions.onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.dish_editor_name)) },
            isError = state.isNameInvalid,
            supportingText = { if (state.isNameInvalid) Text(stringResource(R.string.dish_editor_name_required)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
        OutlinedTextField(
            value = state.description,
            onValueChange = actions.onDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.dish_editor_description)) },
            minLines = 2,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
    }
    SectionCard(title = stringResource(R.string.dish_editor_section_classification)) {
        TypeSelector(selected = state.type, onSelect = actions.onTypeChange)
        HeavinessSelector(selected = state.heaviness, onSelect = actions.onHeavinessChange)
    }
    SectionCard(title = stringResource(R.string.dish_editor_ingredients)) {
        IngredientsSection(state = state, actions = actions)
    }
    SectionCard(title = stringResource(R.string.dish_editor_preparation)) {
        OutlinedTextField(
            value = state.preparation,
            onValueChange = actions.onPreparationChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.dish_editor_preparation)) },
            minLines = 4,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) = Surface(
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    modifier = Modifier.fillMaxWidth()
) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypeSelector(selected: DishType, onSelect: (DishType) -> Unit) = Column(
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    Text(text = stringResource(R.string.dish_editor_type), style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DishType.entries.forEach { type ->
            FilterChip(
                selected = type == selected,
                onClick = { onSelect(type) },
                label = { Text(type.label()) },
                leadingIcon = { Icon(type.icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }
    }
}

@Composable
private fun HeavinessSelector(selected: Heaviness, onSelect: (Heaviness) -> Unit) = Column(
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    Text(text = stringResource(R.string.dish_editor_heaviness), style = MaterialTheme.typography.labelLarge)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        Heaviness.entries.forEachIndexed { index, heaviness ->
            SegmentedButton(
                selected = heaviness == selected,
                onClick = { onSelect(heaviness) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = Heaviness.entries.size),
                icon = { HeavinessDot(heaviness = heaviness) },
                label = { Text(text = heaviness.label(), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        }
    }
}

@Composable
private fun IngredientsSection(state: DishEditorUiState, actions: DishEditorActions) = Column(
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    if (state.ingredients.isEmpty())
        Text(
            text = stringResource(R.string.dish_editor_no_ingredients),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    state.ingredients.forEach { ingredient ->
        key(ingredient.key) {
            IngredientRow(ingredient = ingredient, showErrors = state.showValidationErrors, actions = actions)
        }
    }
    FilledTonalButton(onClick = actions.onAddIngredient) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(text = stringResource(R.string.dish_editor_add_ingredient), modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun IngredientRow(ingredient: IngredientForm, showErrors: Boolean, actions: DishEditorActions) {
    val isNameError = showErrors && !ingredient.isNameValid
    val isQuantityError = showErrors && !ingredient.isQuantityValid
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = ingredient.name,
                    onValueChange = { actions.onIngredientNameChange(ingredient.key, it) },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.dish_editor_ingredient_name)) },
                    isError = isNameError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                IconButton(onClick = { actions.onRemoveIngredient(ingredient.key) }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.dish_editor_remove_ingredient))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = ingredient.quantityText,
                    onValueChange = { actions.onIngredientQuantityChange(ingredient.key, it) },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.dish_editor_ingredient_quantity)) },
                    isError = isQuantityError,
                    supportingText = { if (isQuantityError) Text(stringResource(R.string.dish_editor_invalid_quantity)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                EnumDropdown(
                    label = stringResource(R.string.dish_editor_ingredient_unit),
                    options = MeasureUnit.entries,
                    selected = ingredient.unit,
                    optionLabel = { it.label() },
                    onSelect = { actions.onIngredientUnitChange(ingredient.key, it) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdown(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DishEditorScreenPreview() = MenuPlannerTheme {
    DishEditorScreen(
        state = DishEditorUiState(
            isEditMode = true,
            name = "Tortilla de patatas",
            description = "Jugosa, con cebolla",
            type = DishType.MAIN,
            heaviness = Heaviness.MEDIUM,
            ingredients = listOf(
                IngredientForm(key = 0, name = "Patata", quantityText = "500", unit = MeasureUnit.GRAM),
                IngredientForm(key = 1, name = "Huevo", quantityText = "6", unit = MeasureUnit.UNIT)
            ),
            preparation = "Pelar y freír las patatas..."
        ),
        actions = DishEditorActions()
    )
}
