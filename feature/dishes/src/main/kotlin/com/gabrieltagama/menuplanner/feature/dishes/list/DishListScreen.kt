package com.gabrieltagama.menuplanner.feature.dishes.list

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.ui.component.DishTypeAvatar
import com.gabrieltagama.menuplanner.core.ui.component.EmptyState
import com.gabrieltagama.menuplanner.core.ui.component.HeavinessChip
import com.gabrieltagama.menuplanner.core.ui.component.LoadingIndicator
import com.gabrieltagama.menuplanner.core.ui.text.label
import com.gabrieltagama.menuplanner.core.ui.theme.MenuPlannerTheme
import com.gabrieltagama.menuplanner.feature.dishes.R

/**
 * Recipe book: stateful route bound to [DishListViewModel] and a stateless screen with a search
 * field (dish or ingredient name), one row of dropdown filter chips (type, heaviness) and a list
 * of dish cards with type avatar, ingredient count and heaviness chip. The FAB collapses while
 * scrolling. Bottom insets are left to the app shell, which draws the NavigationBar below.
 */
data class DishListActions(
    val onQueryChange: (String) -> Unit,
    val onTypeSelect: (DishType?) -> Unit,
    val onHeavinessSelect: (Heaviness?) -> Unit,
    val onOpenDish: (String?) -> Unit
)

@Composable
fun DishListScreenRoute(
    onOpenDish: (String?) -> Unit,
    viewModel: DishListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    DishListScreen(
        state = state,
        query = query,
        actions = DishListActions(
            onQueryChange = viewModel::onQueryChange,
            onTypeSelect = viewModel::onTypeSelect,
            onHeavinessSelect = viewModel::onHeavinessSelect,
            onOpenDish = onOpenDish
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishListScreen(
    state: DishListUiState,
    query: String,
    actions: DishListActions
) {
    val listState = rememberLazyListState()
    val isAtTop by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.dishes_title)) }) },
        floatingActionButton = {
            if (state.hasDishes)
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.dishes_add)) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    onClick = { actions.onOpenDish(null) },
                    expanded = isAtTop
                )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SearchField(query = query, onQueryChange = actions.onQueryChange)
            FilterRow(state = state, actions = actions)
            DishListContent(state = state, listState = listState, onOpenDish = actions.onOpenDish)
        }
    }
}

@Composable
private fun DishListContent(state: DishListUiState, listState: LazyListState, onOpenDish: (String?) -> Unit) = when {
    state.isLoading -> LoadingIndicator()
    !state.hasDishes -> EmptyState(
        message = stringResource(R.string.dishes_empty),
        icon = Icons.AutoMirrored.Filled.MenuBook,
        actionLabel = stringResource(R.string.dishes_add),
        onAction = { onOpenDish(null) }
    )
    state.dishes.isEmpty() -> EmptyState(message = stringResource(R.string.dishes_no_results), icon = Icons.Filled.SearchOff)
    else -> DishList(dishes = state.dishes, listState = listState, onOpenDish = onOpenDish)
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) = OutlinedTextField(
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
private fun FilterRow(state: DishListUiState, actions: DishListActions) = Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    DropdownFilterChip(
        title = stringResource(R.string.dishes_column_type),
        allLabel = stringResource(R.string.dishes_filter_all_types),
        options = DishType.entries,
        selected = state.selectedType,
        optionLabel = { it.shortLabel() },
        onSelect = actions.onTypeSelect
    )
    DropdownFilterChip(
        title = stringResource(R.string.dishes_column_heaviness),
        allLabel = stringResource(R.string.dishes_filter_all_heaviness),
        options = Heaviness.entries,
        selected = state.selectedHeaviness,
        optionLabel = { it.label() },
        onSelect = actions.onHeavinessSelect
    )
}

@Composable
private fun <T : Any> DropdownFilterChip(
    title: String,
    allLabel: String,
    options: List<T>,
    selected: T?,
    optionLabel: @Composable (T) -> String,
    onSelect: (T?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val choose: (T?) -> Unit = { option ->
        onSelect(option)
        expanded = false
    }
    Box {
        FilterChip(
            selected = selected != null,
            onClick = { expanded = true },
            label = { Text(selected?.let { optionLabel(it) } ?: title) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(allLabel) }, onClick = { choose(null) })
            options.forEach { option -> DropdownMenuItem(text = { Text(optionLabel(option)) }, onClick = { choose(option) }) }
        }
    }
}

@Composable
private fun DishList(dishes: List<Dish>, listState: LazyListState, onOpenDish: (String?) -> Unit) = LazyColumn(
    state = listState,
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    items(items = dishes, key = { it.id }) { dish ->
        DishCard(dish = dish, onClick = { onOpenDish(dish.id) }, modifier = Modifier.animateItem())
    }
}

@Composable
private fun DishCard(dish: Dish, onClick: () -> Unit, modifier: Modifier = Modifier) = Surface(
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    modifier = modifier.fillMaxWidth()
) {
    ListItem(
        headlineContent = { Text(text = dish.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(text = dish.supportingText(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = { DishTypeAvatar(type = dish.type) },
        trailingContent = { HeavinessChip(heaviness = dish.heaviness) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun Dish.supportingText(): String {
    val typeText = type.shortLabel()
    if (ingredients.isEmpty()) return typeText
    return "$typeText · ${pluralStringResource(R.plurals.dishes_ingredient_count, ingredients.size, ingredients.size)}"
}

@Composable
private fun DishType.shortLabel(): String = stringResource(
    when (this) {
        DishType.STARTER -> R.string.dishes_type_starter_short
        DishType.MAIN -> R.string.dishes_type_main_short
        DishType.DESSERT -> R.string.dishes_type_dessert_short
        DishType.SINGLE -> R.string.dishes_type_single_short
    }
)

private val previewActions = DishListActions(onQueryChange = {}, onTypeSelect = {}, onHeavinessSelect = {}, onOpenDish = {})

private val previewState = DishListUiState(
    isLoading = false,
    hasDishes = true,
    dishes = listOf(
        Dish(
            name = "Ensalada mixta",
            ingredients = listOf(Ingredient("Lechuga", 1.0, MeasureUnit.UNIT)),
            type = DishType.STARTER,
            heaviness = Heaviness.VERY_LOW
        ),
        Dish(name = "Flan de huevo", type = DishType.DESSERT, heaviness = Heaviness.MEDIUM),
        Dish(name = "Lentejas estofadas con chorizo y verduras de temporada", type = DishType.SINGLE, heaviness = Heaviness.VERY_HIGH),
        Dish(name = "Pollo asado", type = DishType.MAIN, heaviness = Heaviness.MEDIUM)
    )
)

@Preview(showBackground = true)
@Composable
private fun DishListScreenPreview() = MenuPlannerTheme {
    DishListScreen(state = previewState, query = "", actions = previewActions)
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DishListScreenDarkPreview() = MenuPlannerTheme {
    DishListScreen(state = previewState, query = "", actions = previewActions)
}

@Preview(showBackground = true)
@Composable
private fun DishListScreenEmptyPreview() = MenuPlannerTheme {
    DishListScreen(state = DishListUiState(isLoading = false), query = "", actions = previewActions)
}
