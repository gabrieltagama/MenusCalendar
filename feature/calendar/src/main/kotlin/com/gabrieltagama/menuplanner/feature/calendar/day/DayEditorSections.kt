package com.gabrieltagama.menuplanner.feature.calendar.day

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.MenuShape
import com.gabrieltagama.menuplanner.core.domain.model.MenuSlot
import com.gabrieltagama.menuplanner.core.ui.component.DishTypeAvatar
import com.gabrieltagama.menuplanner.core.ui.component.HeavinessChip
import com.gabrieltagama.menuplanner.feature.calendar.R

/**
 * Content of the lunch editor: the selector between starter + main and single dish, one card per
 * slot of the active menu shape with the chosen dish, its heaviness and the dinner-dish warning, and
 * the labels of shapes and slots.
 */
@Composable
internal fun DayEditorContent(
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
private fun ModeSelector(selected: MenuShape, onSelect: (MenuShape) -> Unit) =
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        MenuShape.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = MenuShape.entries.size),
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
                    if (selected?.isLunchOption == false)
                        Text(
                            text = stringResource(R.string.day_editor_dinner_dish_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
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
private fun MenuShape.label(): String = stringResource(
    when (this) {
        MenuShape.COURSES -> R.string.day_editor_mode_courses
        MenuShape.SINGLE -> R.string.day_editor_mode_single
    }
)

@Composable
internal fun MenuSlot.label(): String = stringResource(
    when (this) {
        MenuSlot.STARTER -> R.string.calendar_slot_starter
        MenuSlot.MAIN -> R.string.calendar_slot_main
        MenuSlot.SINGLE -> R.string.calendar_slot_single
    }
)
