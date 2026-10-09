package com.gabrieltagama.menuplanner.feature.dishes.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.ui.component.EmptyState
import com.gabrieltagama.menuplanner.core.ui.component.LoadingIndicator
import com.gabrieltagama.menuplanner.core.ui.text.label
import com.gabrieltagama.menuplanner.core.ui.theme.MenuPlannerTheme
import com.gabrieltagama.menuplanner.feature.dishes.R

/**
 * Recipe book list: stateful route bound to [DishListViewModel] and a stateless screen with
 * type filter chips, name search and the list of dish cards. Bottom insets are left to the app
 * shell, which draws the NavigationBar below this screen.
 */
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
        onQueryChange = viewModel::onQueryChange,
        onTypeSelect = viewModel::onTypeSelect,
        onOpenDish = onOpenDish
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishListScreen(
    state: DishListUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onTypeSelect: (DishType?) -> Unit,
    onOpenDish: (String?) -> Unit
) = Scaffold(
    contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
    topBar = { TopAppBar(title = { Text(stringResource(R.string.dishes_title)) }) },
    floatingActionButton = {
        FloatingActionButton(onClick = { onOpenDish(null) }) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.dishes_add))
        }
    }
) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        SearchField(query = query, onQueryChange = onQueryChange)
        TypeFilterRow(selectedType = state.selectedType, onTypeSelect = onTypeSelect)
        DishListContent(state = state, onOpenDish = onOpenDish)
    }
}

@Composable
private fun DishListContent(state: DishListUiState, onOpenDish: (String?) -> Unit) = when {
    state.isLoading -> LoadingIndicator()
    !state.hasDishes -> EmptyState(message = stringResource(R.string.dishes_empty))
    state.dishes.isEmpty() -> EmptyState(message = stringResource(R.string.dishes_no_results))
    else -> LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = state.dishes, key = { it.id }) { dish ->
            DishCard(dish = dish, onClick = { onOpenDish(dish.id) })
        }
    }
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
private fun TypeFilterRow(selectedType: DishType?, onTypeSelect: (DishType?) -> Unit) {
    val options: List<DishType?> = listOf(null) + DishType.entries
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = options, key = { it?.name ?: "ALL" }) { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelect(type) },
                label = { Text(type?.label() ?: stringResource(R.string.dishes_filter_all)) }
            )
        }
    }
}

@Composable
private fun DishCard(dish: Dish, onClick: () -> Unit) = Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = dish.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = dish.type.label(), style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.size(10.dp).background(dish.heaviness.color(), CircleShape))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = dish.heaviness.label(), style = MaterialTheme.typography.labelMedium)
        }
        if (dish.description.isNotBlank())
            Text(
                text = dish.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
    }
}

@Composable
private fun Heaviness.color(): Color = when (this) {
    Heaviness.VERY_LOW -> MaterialTheme.colorScheme.tertiary
    Heaviness.MEDIUM -> MaterialTheme.colorScheme.secondary
    Heaviness.VERY_HIGH -> MaterialTheme.colorScheme.error
}

@Preview(showBackground = true)
@Composable
private fun DishListScreenPreview() = MenuPlannerTheme {
    DishListScreen(
        state = DishListUiState(
            isLoading = false,
            hasDishes = true,
            dishes = listOf(
                Dish(name = "Lentejas estofadas", description = "Con chorizo y verduras", type = DishType.SINGLE, heaviness = Heaviness.VERY_HIGH),
                Dish(name = "Ensalada mixta", type = DishType.STARTER, heaviness = Heaviness.VERY_LOW),
                Dish(name = "Flan de huevo", type = DishType.DESSERT, heaviness = Heaviness.MEDIUM)
            )
        ),
        query = "",
        onQueryChange = {},
        onTypeSelect = {},
        onOpenDish = {}
    )
}
