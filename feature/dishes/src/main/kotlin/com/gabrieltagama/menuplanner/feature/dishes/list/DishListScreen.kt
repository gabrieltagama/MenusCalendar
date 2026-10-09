package com.gabrieltagama.menuplanner.feature.dishes.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.ui.component.EmptyState
import com.gabrieltagama.menuplanner.core.ui.component.LoadingIndicator
import com.gabrieltagama.menuplanner.core.ui.text.label
import com.gabrieltagama.menuplanner.core.ui.theme.MenuPlannerTheme
import com.gabrieltagama.menuplanner.feature.dishes.R

/**
 * Recipe book as a searchable table: stateful route bound to [DishListViewModel] and a stateless
 * screen with a search field (dish or ingredient name), combinable type and heaviness filter
 * chips and a sticky-header table showing only name, type and heaviness. Bottom insets are left
 * to the app shell, which draws the NavigationBar below this screen.
 */
private val TypeColumnWidth: Dp = 96.dp
private val HeavinessColumnWidth: Dp = 96.dp
private val NameColumnMinWidth: Dp = 120.dp
private val RowMinHeight: Dp = 48.dp

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
) = Scaffold(
    contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
    topBar = { TopAppBar(title = { Text(stringResource(R.string.dishes_title)) }) },
    floatingActionButton = {
        ExtendedFloatingActionButton(
            text = { Text(stringResource(R.string.dishes_add)) },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            onClick = { actions.onOpenDish(null) }
        )
    }
) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        SearchField(query = query, onQueryChange = actions.onQueryChange)
        FilterChipRow(
            options = listOf(null) + DishType.entries,
            selected = state.selectedType,
            allLabel = stringResource(R.string.dishes_filter_all_types),
            optionLabel = { it.shortLabel() },
            onSelect = actions.onTypeSelect
        )
        FilterChipRow(
            options = listOf(null) + Heaviness.entries,
            selected = state.selectedHeaviness,
            allLabel = stringResource(R.string.dishes_filter_all_heaviness),
            optionLabel = { it.label() },
            onSelect = actions.onHeavinessSelect
        )
        DishListContent(state = state, onOpenDish = actions.onOpenDish)
    }
}

@Composable
private fun DishListContent(state: DishListUiState, onOpenDish: (String?) -> Unit) = when {
    state.isLoading -> LoadingIndicator()
    !state.hasDishes -> EmptyState(message = stringResource(R.string.dishes_empty))
    state.dishes.isEmpty() -> EmptyState(message = stringResource(R.string.dishes_no_results))
    else -> DishTable(dishes = state.dishes, onOpenDish = onOpenDish)
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
    singleLine = true
)

@Composable
private fun <T : Any> FilterChipRow(
    options: List<T?>,
    selected: T?,
    allLabel: String,
    optionLabel: @Composable (T) -> String,
    onSelect: (T?) -> Unit
) = Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    options.forEach { option ->
        FilterChip(
            selected = selected == option,
            onClick = { onSelect(option) },
            label = { Text(option?.let { optionLabel(it) } ?: allLabel) }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DishTable(dishes: List<Dish>, onOpenDish: (String?) -> Unit) = LazyColumn(
    modifier = Modifier.fillMaxSize().padding(top = 8.dp),
    contentPadding = PaddingValues(bottom = 88.dp)
) {
    stickyHeader(key = "header") { DishTableHeader() }
    items(items = dishes, key = { it.id }) { dish ->
        DishTableRow(dish = dish, onClick = { onOpenDish(dish.id) })
        HorizontalDivider()
    }
}

@Composable
private fun DishTableHeader() = Row(
    modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .heightIn(min = RowMinHeight)
        .padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    HeaderCell(text = stringResource(R.string.dishes_column_name), modifier = nameColumnModifier())
    HeaderCell(text = stringResource(R.string.dishes_column_type), modifier = Modifier.width(TypeColumnWidth))
    HeaderCell(text = stringResource(R.string.dishes_column_heaviness), modifier = Modifier.width(HeavinessColumnWidth))
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier) = Text(
    text = text,
    modifier = modifier,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis
)

@Composable
private fun DishTableRow(dish: Dish, onClick: () -> Unit) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
        .heightIn(min = RowMinHeight)
        .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Text(
        text = dish.name,
        modifier = nameColumnModifier(),
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
    Text(
        text = dish.type.shortLabel(),
        modifier = Modifier.width(TypeColumnWidth),
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    HeavinessCell(heaviness = dish.heaviness, modifier = Modifier.width(HeavinessColumnWidth))
}

@Composable
private fun HeavinessCell(heaviness: Heaviness, modifier: Modifier) = Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically
) {
    Box(modifier = Modifier.size(8.dp).background(heaviness.color(), CircleShape))
    Spacer(modifier = Modifier.width(6.dp))
    Text(
        text = heaviness.label(),
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

private fun RowScope.nameColumnModifier(): Modifier =
    Modifier.weight(1f).widthIn(min = NameColumnMinWidth).padding(end = 8.dp)

@Composable
private fun DishType.shortLabel(): String = stringResource(
    when (this) {
        DishType.STARTER -> R.string.dishes_type_starter_short
        DishType.MAIN -> R.string.dishes_type_main_short
        DishType.DESSERT -> R.string.dishes_type_dessert_short
        DishType.SINGLE -> R.string.dishes_type_single_short
    }
)

@Composable
private fun Heaviness.color(): Color = when (this) {
    Heaviness.VERY_LOW -> MaterialTheme.colorScheme.tertiary
    Heaviness.MEDIUM -> MaterialTheme.colorScheme.secondary
    Heaviness.VERY_HIGH -> MaterialTheme.colorScheme.error
}

private val previewActions = DishListActions(onQueryChange = {}, onTypeSelect = {}, onHeavinessSelect = {}, onOpenDish = {})

@Preview(showBackground = true)
@Composable
private fun DishListScreenPreview() = MenuPlannerTheme {
    DishListScreen(
        state = DishListUiState(
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
        ),
        query = "",
        actions = previewActions
    )
}

@Preview(showBackground = true)
@Composable
private fun DishListScreenNoResultsPreview() = MenuPlannerTheme {
    DishListScreen(
        state = DishListUiState(isLoading = false, hasDishes = true, query = "xyz", selectedHeaviness = Heaviness.VERY_HIGH),
        query = "xyz",
        actions = previewActions
    )
}
