package com.gabrieltagama.menuplanner.feature.share

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.ShareError
import com.gabrieltagama.menuplanner.core.domain.common.fold
import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import com.gabrieltagama.menuplanner.core.domain.usecase.share.ExportMenusUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.share.ImportMenusUseCase
import com.gabrieltagama.menuplanner.core.ui.state.BusyRunner
import com.gabrieltagama.menuplanner.feature.share.files.ShareFiles
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/**
 * Exports the recipe book and calendar to a file to share or to save in a document the user picks,
 * and imports a file picked by the user, through the ShareFiles port. One operation runs at a time; results surface as a nullable
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
    data object Saved : ShareMessage
}

sealed interface ShareEvent {
    data class ShareFile(val uri: Uri) : ShareEvent
}

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val exportMenus: ExportMenusUseCase,
    private val importMenus: ImportMenusUseCase,
    private val files: ShareFiles
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    private val _events = Channel<ShareEvent>(Channel.BUFFERED)
    val events: Flow<ShareEvent> = _events.receiveAsFlow()

    private val busy = BusyRunner(viewModelScope) { isBusy -> _uiState.update { it.copy(isBusy = isBusy) } }

    fun onExport() = runBusy {
        val uri = try {
            files.write(exportMenus())
        } catch (exception: IOException) {
            null
        }
        if (uri == null) showMessage(ShareMessage.ExportFailed)
        else _events.send(ShareEvent.ShareFile(uri))
    }

    fun saveFileName(): String = files.exportFileName()

    fun onSaveTo(uri: Uri) = runBusy {
        val saved = try {
            files.save(uri, exportMenus())
        } catch (exception: IOException) {
            false
        }
        showMessage(if (saved) ShareMessage.Saved else ShareMessage.ExportFailed)
    }

    fun onImport(uri: Uri) = runBusy {
        val text = files.read(uri) ?: return@runBusy showMessage(ShareMessage.Failed(ShareError.InvalidImportFile))
        showMessage(importMenus(text).fold(ShareMessage::Imported, ShareMessage::Failed))
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    private fun runBusy(block: suspend () -> Unit) = busy.run(block)

    private fun showMessage(message: ShareMessage) = _uiState.update { it.copy(message = message) }
}
