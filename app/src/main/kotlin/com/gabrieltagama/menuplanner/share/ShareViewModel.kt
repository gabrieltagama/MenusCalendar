package com.gabrieltagama.menuplanner.share

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import com.gabrieltagama.menuplanner.core.domain.usecase.share.ExportMenusUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.share.ImportMenusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Exports the recipe book and calendar to a JSON file exposed through FileProvider, and imports
 * a JSON picked by the user. File I/O runs on Dispatchers.IO; results surface as a nullable
 * [ShareMessage] in [ShareUiState] and the file to share as a one-shot [ShareEvent].
 */
data class ShareUiState(
    val isBusy: Boolean = false,
    val message: ShareMessage? = null
)

sealed interface ShareMessage {
    data class Imported(val summary: ImportSummary) : ShareMessage
    data class Failed(val error: DomainError) : ShareMessage
    data object ExportFailed : ShareMessage
}

sealed interface ShareEvent {
    data class ShareFile(val uri: Uri) : ShareEvent
}

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val exportMenus: ExportMenusUseCase,
    private val importMenus: ImportMenusUseCase,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    private val _events = Channel<ShareEvent>(Channel.BUFFERED)
    val events: Flow<ShareEvent> = _events.receiveAsFlow()

    fun onExport() = runBusy {
        val uri = try {
            writeExportFile(exportMenus())
        } catch (exception: IOException) {
            null
        }
        if (uri == null) showMessage(ShareMessage.ExportFailed)
        else _events.send(ShareEvent.ShareFile(uri))
    }

    fun onImport(uri: Uri) = runBusy {
        val json = readText(uri)
        if (json == null) {
            showMessage(ShareMessage.Failed(DomainError.InvalidImportFile))
            return@runBusy
        }
        when (val outcome = importMenus(json)) {
            is Outcome.Success -> showMessage(ShareMessage.Imported(outcome.value))
            is Outcome.Failure -> showMessage(ShareMessage.Failed(outcome.error))
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    private fun runBusy(block: suspend () -> Unit) {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            try {
                block()
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    private fun showMessage(message: ShareMessage) = _uiState.update { it.copy(message = message) }

    private suspend fun writeExportFile(json: String): Uri = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, SHARED_DIRECTORY).apply { mkdirs() }
        val fileName = "menuplanner-${LocalDateTime.now().format(FILE_DATE_FORMAT)}.json"
        val file = File(directory, fileName).apply { writeText(json) }
        FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY_SUFFIX", file)
    }

    private suspend fun readText(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        } catch (exception: IOException) {
            null
        } catch (exception: SecurityException) {
            null
        }
    }

    private companion object {
        const val SHARED_DIRECTORY = "shared"
        const val AUTHORITY_SUFFIX = ".fileprovider"
        val FILE_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")
    }
}
