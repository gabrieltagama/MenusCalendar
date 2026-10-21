package com.gabrieltagama.menuplanner.feature.calendar.day

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.MenuSlot
import com.gabrieltagama.menuplanner.core.ui.component.ConfirmDialog
import com.gabrieltagama.menuplanner.core.ui.component.LoadingIndicator
import com.gabrieltagama.menuplanner.core.ui.preview.PreviewDishes
import com.gabrieltagama.menuplanner.core.ui.theme.MenuPlannerTheme
import com.gabrieltagama.menuplanner.feature.calendar.R
import com.gabrieltagama.menuplanner.feature.calendar.common.DishPickerSheet
import com.gabrieltagama.menuplanner.feature.calendar.common.longTitleText
import com.gabrieltagama.menuplanner.feature.calendar.editor.PlanEditorEffects
import java.time.LocalDate

/**
 * Day menu editor: stateful route bound to [DayEditorViewModel] (events, snackbar) and a
 * stateless screen with the mode selector and one card per slot of the active mode. Tapping a
 * card opens a searchable bottom sheet of the dishes of that type; the dice button picks a
 * random one. A dish meant for dinner can be chosen by hand; its card only shows a warning.
 */
@Composable
fun DayEditorScreenRoute(
    onBack: () -> Unit,
    viewModel: DayEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    PlanEditorEffects(viewModel = viewModel, error = state.form.error, snackbarHostState = snackbarHostState, onClose = onBack)

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
