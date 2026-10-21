package com.gabrieltagama.menuplanner.feature.dishes.list

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.model.Rating
import com.gabrieltagama.menuplanner.core.ui.component.DishSortChip
import com.gabrieltagama.menuplanner.core.ui.text.label
import com.gabrieltagama.menuplanner.feature.dishes.R

/**
 * Search and filter controls of the recipe book list: the search field, the sort chip and the
 * multiple-selection chips of type, heaviness, rating and meal time.
 */
@Composable
internal fun SearchField(query: String, onQueryChange: (String) -> Unit) = OutlinedTextField(
    value = query,
    onValueChange = onQueryChange,
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    placeholder = { Text(stringResource(R.string.dishes_search_hint)) },
    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
    trailingIcon = {
        if (query.isNotEmpty())
            IconButton(onClick = { onQueryChange("") }) {
                Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.dishes_clear_search))
            }
    },
    singleLine = true,
    shape = MaterialTheme.shapes.extraLarge
)

@Composable
internal fun FilterRow(state: DishListUiState, actions: DishListActions) = Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    DishSortChip(sort = state.sort, onSortChange = actions.onSortChange)
    MultiSelectFilterChip(
        title = stringResource(R.string.dishes_column_type),
        allLabel = stringResource(R.string.dishes_filter_all_types),
        options = DishType.entries,
        selected = state.selectedTypes,
        optionLabel = { it.shortLabel() },
        onChange = actions.onTypesChange
    )
    MultiSelectFilterChip(
        title = stringResource(R.string.dishes_column_heaviness),
        allLabel = stringResource(R.string.dishes_filter_all_heaviness),
        options = Heaviness.entries,
        selected = state.selectedHeaviness,
        optionLabel = { it.label() },
        onChange = actions.onHeavinessChange
    )
    MultiSelectFilterChip(
        title = stringResource(R.string.dishes_column_rating),
        allLabel = stringResource(R.string.dishes_filter_all_ratings),
        options = Rating.entries.reversed(),
        selected = state.selectedRatings,
        optionLabel = { it.label() },
        onChange = actions.onRatingsChange
    )
    MultiSelectFilterChip(
        title = stringResource(R.string.dishes_column_meal_time),
        allLabel = stringResource(R.string.dishes_filter_all_meal_times),
        options = MealTime.entries,
        selected = state.selectedMealTimes,
        optionLabel = { it.label() },
        onChange = actions.onMealTimesChange
    )
}

@Composable
private fun <T : Any> MultiSelectFilterChip(
    title: String,
    allLabel: String,
    options: List<T>,
    selected: Set<T>,
    optionLabel: @Composable (T) -> String,
    onChange: (Set<T>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val chipLabel = when (selected.size) {
        0 -> title
        1 -> optionLabel(selected.first())
        else -> stringResource(R.string.dishes_filter_selected_count, title, selected.size)
    }
    Box {
        FilterChip(
            selected = selected.isNotEmpty(),
            onClick = { expanded = true },
            label = { Text(chipLabel) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(allLabel) },
                leadingIcon = { Checkbox(checked = selected.isEmpty(), onCheckedChange = null) },
                onClick = { onChange(emptySet()) }
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    leadingIcon = { Checkbox(checked = option in selected, onCheckedChange = null) },
                    onClick = { onChange(if (option in selected) selected - option else selected + option) }
                )
            }
        }
    }
}
