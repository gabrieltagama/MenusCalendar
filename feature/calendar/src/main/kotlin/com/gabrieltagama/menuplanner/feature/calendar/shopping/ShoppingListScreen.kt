package com.gabrieltagama.menuplanner.feature.calendar.shopping

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.RemoveDone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.ShoppingItem
import com.gabrieltagama.menuplanner.core.ui.component.EmptyState
import com.gabrieltagama.menuplanner.core.ui.component.LoadingIndicator
import com.gabrieltagama.menuplanner.core.ui.text.label
import com.gabrieltagama.menuplanner.core.ui.theme.MenuPlannerTheme
import com.gabrieltagama.menuplanner.feature.calendar.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Shopping list of a month: stateful route bound to [ShoppingListViewModel] and a stateless
 * screen listing every ingredient with its total amount. Chips at the top choose whether the list
 * covers lunches, dinners or both, and two date chips open a calendar to pick the first and last
 * day of the range. Each product has a checkbox to tick it as bought (struck through and moved to
 * the end); the top bar unticks everything and shares the pending products as plain text.
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
        onShare = { context.sendText(shareText, chooserTitle) },
        onMealToggle = viewModel::onMealToggle,
        onFromChange = viewModel::onFromChange,
        onToChange = viewModel::onToChange,
        onItemCheck = viewModel::onItemCheck,
        onClearChecks = viewModel::onClearChecks
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    state: ShoppingListUiState,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onMealToggle: (Meal) -> Unit,
    onFromChange: (LocalDate) -> Unit,
    onToChange: (LocalDate) -> Unit,
    onItemCheck: (ShoppingItem, Boolean) -> Unit = { _, _ -> },
    onClearChecks: () -> Unit = {}
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
                IconButton(onClick = onClearChecks, enabled = state.hasChecked) {
                    Icon(Icons.Filled.RemoveDone, contentDescription = stringResource(R.string.shopping_list_clear_checks))
                }
                IconButton(onClick = onShare, enabled = state.canShare) {
                    Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.shopping_list_share))
                }
            }
        )
    }
) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        MealChips(selected = state.selectedMeals, onMealToggle = onMealToggle)
        DateRangeChips(state = state, onFromChange = onFromChange, onToChange = onToChange)
        val contentModifier = Modifier.fillMaxWidth().weight(1f)
        when {
            state.isLoading -> LoadingIndicator(modifier = contentModifier)
            state.items.isEmpty() -> EmptyState(message = stringResource(R.string.shopping_list_empty), modifier = contentModifier)
            else -> ShoppingItems(state = state, onItemCheck = onItemCheck, modifier = contentModifier)
        }
    }
}

@Composable
private fun MealChips(selected: Set<Meal>, onMealToggle: (Meal) -> Unit) = Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    Meal.entries.forEach { meal ->
        FilterChip(
            selected = meal in selected,
            onClick = { onMealToggle(meal) },
            label = { Text(meal.label()) }
        )
    }
}

@Composable
private fun DateRangeChips(
    state: ShoppingListUiState,
    onFromChange: (LocalDate) -> Unit,
    onToChange: (LocalDate) -> Unit
) = Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    DateChip(label = stringResource(R.string.shopping_list_from, state.from.shortText()), date = state.from, onDateChange = onFromChange)
    DateChip(label = stringResource(R.string.shopping_list_to, state.to.shortText()), date = state.to, onDateChange = onToChange)
}

@Composable
private fun DateChip(label: String, date: LocalDate, onDateChange: (LocalDate) -> Unit) {
    var isPicking by remember { mutableStateOf(false) }
    AssistChip(
        onClick = { isPicking = true },
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(18.dp)) }
    )
    if (isPicking)
        DateDialog(
            initial = date,
            onDismiss = { isPicking = false },
            onConfirm = {
                isPicking = false
                onDateChange(it)
            }
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(initial: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initial.toUtcMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { pickerState.selectedDateMillis?.let { onConfirm(it.toUtcDate()) } ?: onDismiss() }) {
                Text(stringResource(R.string.shopping_list_date_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.shopping_list_date_cancel)) }
        }
    ) {
        DatePicker(state = pickerState)
    }
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
private fun ShoppingItems(state: ShoppingListUiState, onItemCheck: (ShoppingItem, Boolean) -> Unit, modifier: Modifier = Modifier) = LazyColumn(modifier = modifier) {
    item {
        Text(
            text = stringResource(R.string.shopping_list_summary, state.rangeText(), state.mealsText()),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
    items(state.sortedItems, key = { "${it.name}|${it.unit}" }) { item ->
        val isChecked = state.isChecked(item)
        Column(modifier = Modifier.animateItem()) {
            ShoppingItemRow(item = item, isChecked = isChecked, onCheck = { onItemCheck(item, it) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun ShoppingItemRow(item: ShoppingItem, isChecked: Boolean, onCheck: (Boolean) -> Unit) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .toggleable(value = isChecked, role = Role.Checkbox, onValueChange = onCheck)
        .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    val color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    val decoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
    Checkbox(checked = isChecked, onCheckedChange = null, modifier = Modifier.padding(12.dp))
    Text(text = item.name, style = MaterialTheme.typography.bodyLarge, color = color, textDecoration = decoration, modifier = Modifier.weight(1f))
    Text(text = item.amountText(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = color, textDecoration = decoration)
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
            from = LocalDate.of(2026, 10, 15),
            to = LocalDate.of(2026, 10, 31),
            isLoading = false,
            items = listOf(
                ShoppingItem(name = "Aceite", quantity = 250.0, unit = MeasureUnit.MILLILITER),
                ShoppingItem(name = "Arroz", quantity = 1.5, unit = MeasureUnit.KILOGRAM),
                ShoppingItem(name = "Pollo", quantity = 2.0, unit = MeasureUnit.UNIT),
                ShoppingItem(name = "Sal", quantity = 15.0, unit = MeasureUnit.GRAM)
            )
        ),
        onBack = {},
        onShare = {},
        onMealToggle = {},
        onFromChange = {},
        onToChange = {}
    )
}
