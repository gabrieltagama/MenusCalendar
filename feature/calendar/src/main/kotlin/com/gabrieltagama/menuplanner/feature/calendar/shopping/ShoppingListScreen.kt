package com.gabrieltagama.menuplanner.feature.calendar.shopping

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.ShoppingItem
import com.gabrieltagama.menuplanner.core.ui.component.EmptyState
import com.gabrieltagama.menuplanner.core.ui.component.LoadingIndicator
import com.gabrieltagama.menuplanner.core.ui.theme.MenuPlannerTheme
import com.gabrieltagama.menuplanner.feature.calendar.R
import com.gabrieltagama.menuplanner.feature.calendar.common.titleText
import java.time.YearMonth

/**
 * Shopping list of a month: stateful route bound to [ShoppingListViewModel] and a stateless
 * screen listing every ingredient with its total amount. The top bar shares the list as plain text.
 */
@Composable
fun ShoppingListScreenRoute(
    onBack: () -> Unit,
    viewModel: ShoppingListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val shareText = state.shareText()
    val chooserTitle = stringResource(R.string.shopping_list_share_chooser)

    ShoppingListScreen(
        state = state,
        onBack = onBack,
        onShare = { context.sendText(shareText, chooserTitle) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    state: ShoppingListUiState,
    onBack: () -> Unit,
    onShare: () -> Unit
) = Scaffold(
    topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.shopping_list_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.shopping_list_back))
                }
            },
            actions = {
                IconButton(onClick = onShare, enabled = state.canShare) {
                    Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.shopping_list_share))
                }
            }
        )
    }
) { padding ->
    val contentModifier = Modifier.fillMaxSize().padding(padding)
    when {
        state.isLoading -> LoadingIndicator(modifier = contentModifier)
        state.items.isEmpty() -> EmptyState(message = stringResource(R.string.shopping_list_empty), modifier = contentModifier)
        else -> ShoppingItems(state = state, modifier = contentModifier)
    }
}

@Composable
private fun ShoppingItems(state: ShoppingListUiState, modifier: Modifier = Modifier) = LazyColumn(modifier = modifier) {
    item {
        Text(
            text = stringResource(R.string.shopping_list_month, state.month.titleText()),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
    items(state.items, key = { "${it.name}|${it.unit}" }) { item ->
        ShoppingItemRow(item = item)
        HorizontalDivider()
    }
}

@Composable
private fun ShoppingItemRow(item: ShoppingItem) = Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text(text = item.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    Text(text = item.amountText(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
}

private fun Context.sendText(text: String, chooserTitle: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(send, chooserTitle))
}

@Preview(showBackground = true)
@Composable
private fun ShoppingListScreenPreview() = MenuPlannerTheme {
    ShoppingListScreen(
        state = ShoppingListUiState(
            month = YearMonth.of(2026, 10),
            isLoading = false,
            items = listOf(
                ShoppingItem(name = "Aceite", quantity = 250.0, unit = MeasureUnit.MILLILITER),
                ShoppingItem(name = "Arroz", quantity = 1.5, unit = MeasureUnit.KILOGRAM),
                ShoppingItem(name = "Pollo", quantity = 2.0, unit = MeasureUnit.UNIT),
                ShoppingItem(name = "Sal", quantity = null, unit = MeasureUnit.TO_TASTE)
            )
        ),
        onBack = {},
        onShare = {}
    )
}
