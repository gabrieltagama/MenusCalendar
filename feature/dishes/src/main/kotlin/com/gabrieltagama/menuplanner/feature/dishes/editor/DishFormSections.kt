package com.gabrieltagama.menuplanner.feature.dishes.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.ui.component.HeavinessDot
import com.gabrieltagama.menuplanner.core.ui.component.icon
import com.gabrieltagama.menuplanner.core.ui.text.label
import com.gabrieltagama.menuplanner.feature.dishes.R

/**
 * Building blocks of the dish editor form: titled section cards, the type and heaviness selectors,
 * the ingredient lines with their quantity and unit, and a generic dropdown for enum values.
 */
@Composable
internal fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) = Surface(
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
internal fun TypeSelector(selected: DishType, onSelect: (DishType) -> Unit) = Column(
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
internal fun HeavinessSelector(selected: Heaviness, onSelect: (Heaviness) -> Unit) = Column(
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
internal fun IngredientsSection(state: DishEditorUiState, actions: DishEditorActions) = Column(
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    if (state.form.ingredients.isEmpty())
        Text(
            text = stringResource(R.string.dish_editor_no_ingredients),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    state.form.ingredients.forEach { ingredient ->
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
                    options = MeasureUnit.STANDARD,
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
internal fun <T> EnumDropdown(
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
