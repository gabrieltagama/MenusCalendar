package com.gabrieltagama.menuplanner.feature.calendar.month

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.model.MenuSlot
import com.gabrieltagama.menuplanner.core.domain.model.plan.DishSummary
import com.gabrieltagama.menuplanner.core.domain.model.plan.PlannedLunch
import com.gabrieltagama.menuplanner.core.ui.component.DishTypeAvatar
import com.gabrieltagama.menuplanner.core.ui.component.HeavinessChip
import com.gabrieltagama.menuplanner.feature.calendar.R
import com.gabrieltagama.menuplanner.feature.calendar.common.longTitleText
import java.time.LocalDate

/**
 * Animated summary card of the selected day in the active view: the planned dishes with their
 * course label, type avatar and heaviness, or an empty state; its button opens the lunch or the
 * dinner editor of that day.
 */
@Composable
internal fun DaySummaryCard(date: LocalDate, lines: List<Pair<String, DishSummary>>, view: Meal, onOpen: () -> Unit) = Surface(
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surfaceContainer,
    modifier = Modifier.fillMaxWidth()
) {
    AnimatedContent(targetState = Triple(date, view, lines), transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "day") { (shownDate, shownView, shownLines) ->
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = shownDate.longTitleText().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleLarge
            )
            if (shownLines.isEmpty()) EmptyDaySummary(view = shownView, onOpenLunch = onOpen)
            else PlannedDaySummary(lines = shownLines, onOpenLunch = onOpen)
        }
    }
}

@Composable
internal fun CalendarUiState.summaryLines(): List<Pair<String, DishSummary>> = when (view) {
    Meal.LUNCH -> selectedLunch?.summaryLines().orEmpty()
    Meal.DINNER -> listOfNotNull(selectedDinner?.let { stringResource(R.string.calendar_slot_dinner) to it.dish })
}

@Composable
private fun EmptyDaySummary(view: Meal, onOpenLunch: () -> Unit) = Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Filled.EventBusy, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = stringResource(if (view == Meal.LUNCH) R.string.calendar_no_menu else R.string.calendar_no_dinner),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    FilledTonalButton(onClick = onOpenLunch, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.size(8.dp))
        Text(stringResource(R.string.calendar_plan))
    }
}

@Composable
private fun PlannedDaySummary(lines: List<Pair<String, DishSummary>>, onOpenLunch: () -> Unit) = Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    lines.forEach { (label, dish) -> SummaryLine(label = label, dish = dish) }
    OutlinedButton(onClick = onOpenLunch, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.size(8.dp))
        Text(stringResource(R.string.calendar_edit))
    }
}

@Composable
private fun SummaryLine(label: String, dish: DishSummary) = Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
) {
    DishTypeAvatar(type = dish.type, size = 36.dp)
    Column(modifier = Modifier.weight(1f)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = dish.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
    HeavinessChip(heaviness = dish.heaviness)
}

@Composable
private fun PlannedLunch.summaryLines(): List<Pair<String, DishSummary>> =
    slots.map { (slot, dish) -> stringResource(slot.labelRes()) to dish }

private fun MenuSlot.labelRes(): Int = when (this) {
    MenuSlot.STARTER -> R.string.calendar_slot_starter
    MenuSlot.MAIN -> R.string.calendar_slot_main
    MenuSlot.SINGLE -> R.string.calendar_slot_single
}
