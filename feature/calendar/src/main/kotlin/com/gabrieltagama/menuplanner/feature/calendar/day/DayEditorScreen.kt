package com.gabrieltagama.menuplanner.feature.calendar.day

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrieltagama.menuplanner.core.domain.common.MenuSlot
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.ui.component.ConfirmDialog
import com.gabrieltagama.menuplanner.core.ui.component.LoadingIndicator
import com.gabrieltagama.menuplanner.core.ui.text.label
import com.gabrieltagama.menuplanner.core.ui.text.message
import com.gabrieltagama.menuplanner.core.ui.theme.MenuPlannerTheme
import com.gabrieltagama.menuplanner.feature.calendar.R
import com.gabrieltagama.menuplanner.feature.calendar.common.longTitleText
import java.time.LocalDate

/**
 * Day menu editor: stateful route bound to [DayEditorViewModel] (events, snackbar) and a
 * stateless screen with the mode selector and one dish selector per slot of the active mode.
 */
data class DayEditorActions(
    val onBack: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onModeChange: (MenuMode) -> Unit = {},
    val onDishSelect: (MenuSlot, Dish?) -> Unit = { _, _ -> },
    val onClearRequest: () -> Unit = {},
    val onClearConfirm: () -> Unit = {},
    val onClearDismiss: () -> Unit = {}
)

@Composable
fun DayEditorScreenRoute(
    onBack: () -> Unit,
    viewModel: DayEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = state.form.error?.message()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                DayEditorEvent.Close -> currentOnBack()
            }
        }
    }
    LaunchedEffect(errorMessage) {
        val message = errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.onErrorShown()
    }

    DayEditorScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        actions = DayEditorActions(
            onBack = onBack,
            onSave = viewModel::onSave,
            onModeChange = viewModel::onModeChange,
            onDishSelect = viewModel::onDishSelect,
            onClearRequest = viewModel::onClearRequest,
            onClearConfirm = viewModel::onClearConfirm,
            onClearDismiss = viewModel::onClearDismiss
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayEditorScreen(
    state: DayEditorUiState,
    actions: DayEditorActions,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = state.date.longTitleText(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.day_editor_back))
                    }
                },
                actions = {
                    if (state.form.hasSavedDay)
                        IconButton(onClick = actions.onClearRequest, enabled = !state.form.isSaving) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = stringResource(R.string.day_editor_clear))
                        }
                    TextButton(onClick = actions.onSave, enabled = state.canSave) {
                        Text(stringResource(R.string.day_editor_save))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val contentModifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
        if (state.form.isLoading || state.isOptionsLoading) LoadingIndicator(modifier = contentModifier)
        else DayEditorContent(state = state, actions = actions, modifier = contentModifier)
    }

    if (state.form.showClearConfirm)
        ConfirmDialog(
            title = stringResource(R.string.day_editor_clear_title),
            text = stringResource(R.string.day_editor_clear_text),
            onConfirm = actions.onClearConfirm,
            onDismiss = actions.onClearDismiss,
            confirmLabel = stringResource(R.string.day_editor_clear)
        )
}

@Composable
private fun DayEditorContent(state: DayEditorUiState, actions: DayEditorActions, modifier: Modifier = Modifier) = Column(
    modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
) {
    ModeSelector(selected = state.form.mode, onSelect = actions.onModeChange)
    state.visibleSlots.forEach { slot ->
        SlotSelector(
            slot = slot,
            options = state.optionsFor(slot),
            selected = state.selectionFor(slot),
            onSelect = { actions.onDishSelect(slot, it) }
        )
    }
}

@Composable
private fun ModeSelector(selected: MenuMode, onSelect: (MenuMode) -> Unit) =
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        MenuMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = MenuMode.entries.size),
                label = { Text(mode.label()) }
            )
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotSelector(slot: MenuSlot, options: List<Dish>, selected: Dish?, onSelect: (Dish?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val slotLabel = slot.label()
    val fieldLabel = if (slot.isRequired) stringResource(R.string.day_editor_required, slotLabel) else slotLabel
    val placeholder = stringResource(if (slot.isRequired) R.string.day_editor_select_dish else R.string.day_editor_no_dessert)
    val hasOptions = options.isNotEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it && hasOptions }) {
            OutlinedTextField(
                value = selected?.name ?: placeholder,
                onValueChange = {},
                readOnly = true,
                enabled = hasOptions || selected != null,
                singleLine = true,
                label = { Text(fieldLabel) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (!slot.isRequired)
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.day_editor_no_dessert)) },
                        onClick = {
                            onSelect(null)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                options.forEach { dish ->
                    DropdownMenuItem(
                        text = { Text(text = dish.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        trailingIcon = { Text(text = dish.heaviness.label(), style = MaterialTheme.typography.labelSmall) },
                        onClick = {
                            onSelect(dish)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
        if (!hasOptions)
            Text(
                text = stringResource(R.string.day_editor_no_dishes_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
    }
}

@Composable
private fun MenuMode.label(): String = stringResource(
    when (this) {
        MenuMode.COURSES -> R.string.day_editor_mode_courses
        MenuMode.SINGLE -> R.string.day_editor_mode_single
    }
)

@Composable
private fun MenuSlot.label(): String = stringResource(
    when (this) {
        MenuSlot.STARTER -> R.string.calendar_slot_starter
        MenuSlot.MAIN -> R.string.calendar_slot_main
        MenuSlot.SINGLE -> R.string.calendar_slot_single
        MenuSlot.DESSERT -> R.string.calendar_slot_dessert
    }
)

@Preview(showBackground = true)
@Composable
private fun DayEditorScreenPreview() = MenuPlannerTheme {
    val starter = Dish(name = "Crema de calabaza", type = DishType.STARTER, heaviness = Heaviness.VERY_LOW)
    val main = Dish(name = "Merluza en salsa verde", type = DishType.MAIN, heaviness = Heaviness.MEDIUM)
    DayEditorScreen(
        state = DayEditorUiState(
            date = LocalDate.of(2026, 10, 9),
            form = DayEditorForm(isLoading = false, selections = mapOf(MenuSlot.STARTER to starter, MenuSlot.MAIN to main)),
            options = mapOf(DishType.STARTER to listOf(starter), DishType.MAIN to listOf(main)),
            isOptionsLoading = false
        ),
        actions = DayEditorActions()
    )
}
