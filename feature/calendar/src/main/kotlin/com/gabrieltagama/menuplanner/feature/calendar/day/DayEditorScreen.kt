package com.gabrieltagama.menuplanner.feature.calendar.day

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.MenuSlot
import com.gabrieltagama.menuplanner.core.ui.component.ConfirmDialog
import com.gabrieltagama.menuplanner.core.ui.component.DishTypeAvatar
import com.gabrieltagama.menuplanner.core.ui.component.HeavinessChip
import com.gabrieltagama.menuplanner.core.ui.component.LoadingIndicator
import com.gabrieltagama.menuplanner.core.ui.preview.PreviewDishes
import com.gabrieltagama.menuplanner.core.ui.text.message
import com.gabrieltagama.menuplanner.core.ui.theme.MenuPlannerTheme
import com.gabrieltagama.menuplanner.feature.calendar.R
import com.gabrieltagama.menuplanner.feature.calendar.common.DishPickerSheet
import com.gabrieltagama.menuplanner.feature.calendar.common.longTitleText
import com.gabrieltagama.menuplanner.feature.calendar.editor.PlanEditorEvent
import java.time.LocalDate

/**
 * Day menu editor: stateful route bound to [DayEditorViewModel] (events, snackbar) and a
 * stateless screen with the mode selector and one card per slot of the active mode. Tapping a
 * card opens a searchable bottom sheet of the dishes of that type; the dice button picks a
 * random one.
 */
data class DayEditorActions(
    val onBack: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onModeChange: (MenuMode) -> Unit = {},
    val onDishSelect: (MenuSlot, Dish?) -> Unit = { _, _ -> },
    val onRandomDish: (MenuSlot) -> Unit = {},
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
                PlanEditorEvent.Close -> currentOnBack()
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
            onRandomDish = viewModel::onRandomDish,
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
    var pickingSlot by rememberSaveable { mutableStateOf<MenuSlot?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.date.longTitleText().replaceFirstChar { it.uppercase() },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
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
        else DayEditorContent(state = state, actions = actions, onPick = { pickingSlot = it }, modifier = contentModifier)
    }

    pickingSlot?.let { slot ->
        DishPickerSheet(
            title = slot.label(),
            noneLabel = null,
            options = state.optionsFor(slot),
            selected = state.selectionFor(slot),
            onSelect = { actions.onDishSelect(slot, it) },
            onDismiss = { pickingSlot = null },
            usage = state.usage
        )
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
private fun DayEditorContent(
    state: DayEditorUiState,
    actions: DayEditorActions,
    onPick: (MenuSlot) -> Unit,
    modifier: Modifier = Modifier
) = Column(
    modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
) {
    ModeSelector(selected = state.form.mode, onSelect = actions.onModeChange)
    state.visibleSlots.forEach { slot ->
        SlotCard(
            slot = slot,
            selected = state.selectionFor(slot),
            hasOptions = state.optionsFor(slot).isNotEmpty(),
            onPick = { onPick(slot) },
            onRandom = { actions.onRandomDish(slot) }
        )
    }
}

@Composable
private fun ModeSelector(selected: MenuMode, onSelect: (MenuMode) -> Unit) =
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        MenuMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = MenuMode.entries.size),
                label = { Text(mode.label()) }
            )
        }
    }

@Composable
private fun SlotCard(slot: MenuSlot, selected: Dish?, hasOptions: Boolean, onPick: () -> Unit, onRandom: () -> Unit) {
    val slotLabel = slot.label()
    val title = stringResource(R.string.day_editor_required, slotLabel)
    val placeholder = stringResource(R.string.day_editor_select_dish)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (selected != null) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = hasOptions, onClick = onPick)
                    .heightIn(min = 72.dp)
                    .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DishTypeAvatar(type = slot.dishType)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = selected?.name ?: placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (selected != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    selected?.let { HeavinessChip(heaviness = it.heaviness, modifier = Modifier.padding(top = 4.dp)) }
                }
                IconButton(onClick = onRandom, enabled = hasOptions) {
                    Icon(Icons.Filled.Casino, contentDescription = stringResource(R.string.day_editor_random))
                }
                IconButton(onClick = onPick, enabled = hasOptions) {
                    Icon(Icons.Filled.UnfoldMore, contentDescription = stringResource(R.string.day_editor_change))
                }
            }
            if (!hasOptions)
                Text(
                    text = stringResource(R.string.day_editor_no_dishes_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )
        }
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
    }
)

@Preview(showBackground = true)
@Composable
private fun DayEditorScreenPreview() = MenuPlannerTheme {
    val starter = PreviewDishes.dish(name = "Crema de calabaza", type = DishType.STARTER, heaviness = Heaviness.VERY_LOW)
    val main = PreviewDishes.dish(name = "Merluza en salsa verde", type = DishType.MAIN, heaviness = Heaviness.MEDIUM)
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
