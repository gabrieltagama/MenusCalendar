package com.gabrieltagama.menuplanner.feature.calendar.month

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.feature.calendar.R

/**
 * Lunches/dinners switch of the calendar: a Switch between two tappable labels with their icons,
 * the active one highlighted.
 */
@Composable
internal fun MealViewSwitch(view: Meal, onViewChange: (Meal) -> Unit) = Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
) {
    MealViewLabel(
        text = stringResource(R.string.calendar_view_lunch),
        icon = Icons.Filled.WbSunny,
        isActive = view == Meal.LUNCH,
        onClick = { onViewChange(Meal.LUNCH) }
    )
    Switch(
        checked = view == Meal.DINNER,
        onCheckedChange = { isDinner -> onViewChange(if (isDinner) Meal.DINNER else Meal.LUNCH) },
        thumbContent = {
            Icon(
                imageVector = if (view == Meal.DINNER) Icons.Filled.Bedtime else Icons.Filled.WbSunny,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize)
            )
        },
        colors = SwitchDefaults.colors(
            uncheckedThumbColor = MaterialTheme.colorScheme.primary,
            uncheckedTrackColor = MaterialTheme.colorScheme.primaryContainer,
            uncheckedIconColor = MaterialTheme.colorScheme.onPrimary,
            uncheckedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
    MealViewLabel(
        text = stringResource(R.string.calendar_view_dinner),
        icon = Icons.Filled.Bedtime,
        isActive = view == Meal.DINNER,
        onClick = { onViewChange(Meal.DINNER) }
    )
}

@Composable
private fun MealViewLabel(text: String, icon: ImageVector, isActive: Boolean, onClick: () -> Unit) = Row(
    modifier = Modifier
        .clip(RoundedCornerShape(12.dp))
        .clickable(onClick = onClick)
        .padding(horizontal = 8.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp)
) {
    val color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
        color = color
    )
}
