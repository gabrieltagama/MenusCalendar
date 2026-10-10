package com.gabrieltagama.menuplanner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gabrieltagama.menuplanner.R
import com.gabrieltagama.menuplanner.core.ui.theme.MenuPlannerTheme
import kotlinx.coroutines.launch

/**
 * Bottom sheet opened from the "Más" tab with the secondary actions: export JSON and import
 * JSON. The sheet is hidden with its animation before the chosen action runs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MoreActionsSheet(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val closeThen: (() -> Unit) -> Unit = { action ->
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            action()
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        MoreActionsContent(
            isBusy = isBusy,
            onExport = { closeThen(onExport) },
            onImport = { closeThen(onImport) }
        )
    }
}

@Composable
private fun MoreActionsContent(
    isBusy: Boolean,
    onExport: () -> Unit,
    onImport: () -> Unit
) = Column(modifier = Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
    if (isBusy) LinearProgressIndicator(modifier = Modifier.padding(horizontal = 16.dp))
    ActionItem(Icons.Filled.FileUpload, stringResource(R.string.more_export_json), enabled = !isBusy, onClick = onExport)
    ActionItem(Icons.Filled.FileDownload, stringResource(R.string.more_import_json), enabled = !isBusy, onClick = onImport)
}

@Composable
private fun ActionItem(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) = ListItem(
    headlineContent = { Text(label) },
    leadingContent = { Icon(icon, contentDescription = null) },
    modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
)

@Preview(showBackground = true)
@Composable
private fun MoreActionsContentPreview() = MenuPlannerTheme {
    MoreActionsContent(isBusy = false, onExport = {}, onImport = {})
}
