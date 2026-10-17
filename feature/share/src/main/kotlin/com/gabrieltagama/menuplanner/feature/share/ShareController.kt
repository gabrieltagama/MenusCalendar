package com.gabrieltagama.menuplanner.feature.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Wires the share flow into a screen: it opens the system share sheet for an exported file, the
 * document picker for an import and shows the result in the given snackbar host. Callers only
 * need isBusy, onExport and onImport.
 */
class ShareController(
    val isBusy: Boolean,
    val onExport: () -> Unit,
    val onImport: () -> Unit
)

@Composable
fun rememberShareController(snackbarHostState: SnackbarHostState, viewModel: ShareViewModel = hiltViewModel()): ShareController {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chooserTitle = stringResource(R.string.share_chooser_title)
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::onImport)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ShareEvent.ShareFile -> context.shareJson(event.uri, chooserTitle)
            }
        }
    }

    val messageText = state.message?.text()
    LaunchedEffect(state.message) {
        if (messageText == null) return@LaunchedEffect
        snackbarHostState.showSnackbar(messageText)
        viewModel.onMessageShown()
    }

    return ShareController(isBusy = state.isBusy, onExport = viewModel::onExport, onImport = { importLauncher.launch(IMPORT_MIME_TYPES) })
}

private fun Context.shareJson(uri: Uri, chooserTitle: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = JSON_MIME_TYPE
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(send, chooserTitle))
}

private val IMPORT_MIME_TYPES = arrayOf("application/json", "text/plain", "application/octet-stream")
private const val JSON_MIME_TYPE = "application/json"
