package com.gabrieltagama.menuplanner.feature.dishes.list

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import com.gabrieltagama.menuplanner.core.domain.model.DishSort
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.DishUsage
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.Rating
import com.gabrieltagama.menuplanner.core.ui.component.DishTypeAvatar
import com.gabrieltagama.menuplanner.core.ui.component.EmptyState
import com.gabrieltagama.menuplanner.core.ui.component.HeavinessChip
import com.gabrieltagama.menuplanner.core.ui.component.LoadingIndicator
import com.gabrieltagama.menuplanner.core.ui.preview.PreviewDishes
import com.gabrieltagama.menuplanner.core.ui.text.lastServedText
import com.gabrieltagama.menuplanner.core.ui.theme.MenuPlannerTheme
import com.gabrieltagama.menuplanner.feature.dishes.R

/**
 * Recipe book: stateful route bound to [DishListViewModel] and a stateless screen with a search
 * field (dish or ingredient name), one scrollable row of multiple-selection filter chips (type,
 * heaviness, rating, meal time; the menu stays open while ticking options) and a list of dish cards with type avatar, ingredient count and
 * heaviness chip. The FAB collapses while scrolling. Bottom insets are left to the app shell, which draws the NavigationBar below.
 */
data class DishListActions(
    val onQueryChange: (String) -> Unit,
    val onTypesChange: (Set<DishType>) -> Unit,
    val onHeavinessChange: (Set<Heaviness>) -> Unit,
    val onRatingsChange: (Set<Rating>) -> Unit,
    val onMealTimesChange: (Set<MealTime>) -> Unit,
    val onOpenDish: (String?) -> Unit,
    val onSortChange: (DishSort) -> Unit = {}
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
            onTypesChange = viewModel::onTypesChange,
            onHeavinessChange = viewModel::onHeavinessChange,
            onRatingsChange = viewModel::onRatingsChange,
            onMealTimesChange = viewModel::onMealTimesChange,
            onOpenDish = onOpenDish,
            onSortChange = viewModel::onSortChange
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
    else -> DishList(dishes = state.dishes, usage = state.usage.takeIf { state.sort == DishSort.LAST_SERVED }, listState = listState, onOpenDish = onOpenDish)
}

@Composable
private fun DishList(dishes: List<Dish>, usage: DishUsage?, listState: LazyListState, onOpenDish: (String?) -> Unit) = LazyColumn(
    state = listState,
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    items(items = dishes, key = { it.id.value }) { dish ->
        DishCard(dish = dish, usage = usage, onClick = { onOpenDish(dish.id.value) }, modifier = Modifier.animateItem())
    }
}

@Composable
private fun DishCard(dish: Dish, usage: DishUsage?, onClick: () -> Unit, modifier: Modifier = Modifier) = Surface(
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    modifier = modifier.fillMaxWidth()
) {
    ListItem(
        headlineContent = { Text(text = dish.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            val supporting = usage?.let { "${dish.supportingText()} · ${it.lastServedText(dish.id)}" } ?: dish.supportingText()
            Text(text = supporting, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
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
internal fun DishType.shortLabel(): String = stringResource(
    when (this) {
        DishType.STARTER -> R.string.dishes_type_starter_short
        DishType.MAIN -> R.string.dishes_type_main_short
        DishType.DESSERT -> R.string.dishes_type_dessert_short
        DishType.SINGLE -> R.string.dishes_type_single_short
    }
)

private val previewActions = DishListActions(
    onQueryChange = {},
    onTypesChange = {},
    onHeavinessChange = {},
    onRatingsChange = {},
    onMealTimesChange = {},
    onOpenDish = {}
)

private val previewState = DishListUiState(
    isLoading = false,
    hasDishes = true,
    dishes = listOf(
        PreviewDishes.dish(
            name = "Ensalada mixta",
            ingredients = listOf(Ingredient("Lechuga", 1.0, MeasureUnit.UNIT)),
            type = DishType.STARTER,
            heaviness = Heaviness.VERY_LOW
        ),
        PreviewDishes.dish(name = "Flan de huevo", type = DishType.DESSERT, heaviness = Heaviness.MEDIUM),
        PreviewDishes.dish(name = "Lentejas estofadas con chorizo y verduras de temporada", type = DishType.SINGLE, heaviness = Heaviness.VERY_HIGH),
        PreviewDishes.dish(name = "Pollo asado", type = DishType.MAIN, heaviness = Heaviness.MEDIUM)
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
