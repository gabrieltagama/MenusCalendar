package com.gabrieltagama.menuplanner.feature.calendar.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishSort
import com.gabrieltagama.menuplanner.core.domain.model.DishUsage
import com.gabrieltagama.menuplanner.core.domain.text.SearchableDish
import com.gabrieltagama.menuplanner.core.domain.text.normalizedForSearch
import com.gabrieltagama.menuplanner.core.ui.component.DishSortChip
import com.gabrieltagama.menuplanner.core.ui.component.DishTypeAvatar
import com.gabrieltagama.menuplanner.core.ui.component.HeavinessChip
import com.gabrieltagama.menuplanner.core.ui.text.lastServedText
import com.gabrieltagama.menuplanner.feature.calendar.R
import kotlinx.coroutines.launch

/**
 * Searchable bottom sheet to choose one dish, shared by the lunch and dinner editors. The search
 * ignores case and accents and also looks into ingredients; a chip orders the list (name, last
 * time served, rating or heaviness) and every row says when the dish was last served. When
 * noneLabel is given, a first row lets the user leave the slot empty. The search texts of the options
 * are normalized once per options list (SearchableDish), not on every keystroke.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DishPickerSheet(
    title: String,
    noneLabel: String?,
    options: List<Dish>,
    selected: Dish?,
    onSelect: (Dish?) -> Unit,
    onDismiss: () -> Unit,
    usage: DishUsage = DishUsage.NONE
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(DishSort.NAME) }
    val index = remember(options) { options.map(::SearchableDish) }
    val visible = remember(index, query, sort, usage) {
        val normalizedQuery = query.normalizedForSearch()
        sort.sorted(index.filter { it.matches(normalizedQuery) }.map(SearchableDish::dish), usage)
    }
    val choose: (Dish?) -> Unit = { dish ->
        onSelect(dish)
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = { Text(stringResource(R.string.day_editor_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty())
                        IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, contentDescription = null) }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge
            )
            DishSortChip(sort = sort, onSortChange = { sort = it }, modifier = Modifier.padding(horizontal = 16.dp))
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                if (noneLabel != null)
                    item(key = "none") {
                        PickerRow(
                            headline = noneLabel,
                            leading = { Icon(Icons.Filled.DoNotDisturbOn, contentDescription = null) },
                            isSelected = selected == null,
                            onClick = { choose(null) }
                        )
                    }
                items(items = visible, key = { it.id.value }) { dish ->
                    PickerRow(
                        headline = dish.name,
                        supporting = usage.lastServedText(dish.id),
                        leading = { DishTypeAvatar(type = dish.type, size = 36.dp) },
                        trailing = { HeavinessChip(heaviness = dish.heaviness) },
                        isSelected = dish.id == selected?.id,
                        onClick = { choose(dish) }
                    )
                }
                if (visible.isEmpty())
                    item(key = "empty") {
                        Text(
                            text = stringResource(R.string.day_editor_no_matches),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
            }
        }
    }
}

@Composable
private fun PickerRow(
    headline: String,
    leading: @Composable () -> Unit,
    isSelected: Boolean,
    onClick: () -> Unit,
    supporting: String? = null,
    trailing: @Composable () -> Unit = {}
) = ListItem(
    headlineContent = { Text(text = headline, maxLines = 2, overflow = TextOverflow.Ellipsis) },
    supportingContent = if (supporting == null) {
        null
    } else {
        { Text(text = supporting, maxLines = 1) }
    },
    leadingContent = leading,
    trailingContent = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            trailing()
            if (isSelected) Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    },
    colors = ListItemDefaults.colors(
        containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
    ),
    modifier = Modifier.clickable(onClick = onClick)
)
