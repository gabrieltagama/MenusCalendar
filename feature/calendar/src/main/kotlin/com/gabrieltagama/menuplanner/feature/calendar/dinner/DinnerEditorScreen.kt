package com.gabrieltagama.menuplanner.feature.calendar.dinner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
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
 * Dinner editor: stateful route bound to [DinnerEditorViewModel] (events, snackbar) and a
 * stateless screen with one card for the single dinner dish. Tapping the card opens the
 * searchable list of dinner options; the dice button picks a random one.
 */
data class DinnerEditorActions(
    val onBack: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onDishSelect: (Dish?) -> Unit = {},
    val onRandomDish: () -> Unit = {},
    val onClearRequest: () -> Unit = {},
    val onClearConfirm: () -> Unit = {},
    val onClearDismiss: () -> Unit = {}
)

@Composable
fun DinnerEditorScreenRoute(
    onBack: () -> Unit,
    viewModel: DinnerEditorViewModel = hiltViewModel()
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

    DinnerEditorScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        actions = DinnerEditorActions(
            onBack = onBack,
            onSave = viewModel::onSave,
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
fun DinnerEditorScreen(
    state: DinnerEditorUiState,
    actions: DinnerEditorActions,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    var isPicking by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.dinner_editor_title, state.date.longTitleText()),
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
                    if (state.form.hasSavedDinner)
                        IconButton(onClick = actions.onClearRequest, enabled = !state.form.isSaving) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = stringResource(R.string.dinner_editor_clear))
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
        else
            Column(
                modifier = contentModifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DinnerDishCard(
                    selected = state.form.selected,
                    hasOptions = state.options.isNotEmpty(),
                    onPick = { isPicking = true },
                    onRandom = actions.onRandomDish
                )
                Text(
                    text = stringResource(R.string.dinner_editor_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
    }

    if (isPicking)
        DishPickerSheet(
            title = stringResource(R.string.dinner_editor_dish),
            noneLabel = null,
            options = state.options,
            selected = state.form.selected,
            onSelect = actions.onDishSelect,
            onDismiss = { isPicking = false },
            usage = state.usage
        )

    if (state.form.showClearConfirm)
        ConfirmDialog(
            title = stringResource(R.string.dinner_editor_clear),
            text = stringResource(R.string.dinner_editor_clear_text),
            onConfirm = actions.onClearConfirm,
            onDismiss = actions.onClearDismiss,
            confirmLabel = stringResource(R.string.dinner_editor_clear)
        )
}

@Composable
private fun DinnerDishCard(selected: Dish?, hasOptions: Boolean, onPick: () -> Unit, onRandom: () -> Unit) = Surface(
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
            if (selected != null) DishTypeAvatar(type = selected.type) else DinnerAvatar()
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.day_editor_required, stringResource(R.string.dinner_editor_dish)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = selected?.name ?: stringResource(R.string.day_editor_select_dish),
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
                text = stringResource(R.string.dinner_editor_no_dishes_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            )
    }
}

@Composable
private fun DinnerAvatar() = Box(
    modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
    contentAlignment = Alignment.Center
) {
    Icon(Icons.Filled.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(22.dp))
}

@Preview(showBackground = true)
@Composable
private fun DinnerEditorScreenPreview() = MenuPlannerTheme {
    val omelette = PreviewDishes.dish(name = "Tortilla francesa", type = DishType.MAIN, heaviness = Heaviness.VERY_LOW, mealTime = MealTime.DINNER)
    DinnerEditorScreen(
        state = DinnerEditorUiState(
            date = LocalDate.of(2026, 10, 9),
            form = DinnerEditorForm(isLoading = false, selected = omelette),
            options = listOf(omelette),
            isOptionsLoading = false
        ),
        actions = DinnerEditorActions()
    )
}
