package com.gabrieltagama.menuplanner.feature.calendar.month

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabrieltagama.menuplanner.feature.calendar.R

/**
 * Bottom bar of the selection mode with the actions on the marked days of the active view: fill
 * the empty ones, copy them or delete the planned ones (shown as destructive).
 */
@Composable
internal fun SelectionActionBar(hasMarkedDays: Boolean, actions: SelectionActions) = Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    modifier = Modifier.fillMaxWidth()
) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SelectionActionButton(text = stringResource(R.string.calendar_selection_fill), icon = Icons.Filled.AutoAwesome, enabled = hasMarkedDays, onClick = actions.onFill)
        SelectionActionButton(text = stringResource(R.string.calendar_selection_copy), icon = Icons.Filled.ContentCopy, enabled = hasMarkedDays, onClick = actions.onCopy)
        SelectionActionButton(
            text = stringResource(R.string.calendar_selection_delete),
            icon = Icons.Filled.Delete,
            enabled = hasMarkedDays,
            onClick = actions.onDelete,
            isDestructive = true
        )
    }
}

@Composable
private fun RowScope.SelectionActionButton(text: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit, isDestructive: Boolean = false) = FilledTonalButton(
    onClick = onClick,
    enabled = enabled,
    modifier = Modifier.weight(1f),
    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
    colors = if (isDestructive)
        ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
    else ButtonDefaults.filledTonalButtonColors()
) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(modifier = Modifier.width(6.dp))
    Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis)
}
