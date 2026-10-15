package com.gabrieltagama.menuplanner.share

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import com.gabrieltagama.menuplanner.core.domain.usecase.share.ExportMenusUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.share.ImportMenusFileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Exports the recipe book and calendar to a shareable JSON file and imports a JSON picked by the
 * user, both through domain use cases (the file handling lives behind ShareFileStore). Results
 * surface as a nullable [ShareMessage] in [ShareUiState] and the file to share as a one-shot
 * [ShareEvent].
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
    private val importMenusFile: ImportMenusFileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    private val _events = Channel<ShareEvent>(Channel.BUFFERED)
    val events: Flow<ShareEvent> = _events.receiveAsFlow()

    fun onExport() = runBusy {
        val location = exportMenus()
        if (location == null) showMessage(ShareMessage.ExportFailed)
        else _events.send(ShareEvent.ShareFile(Uri.parse(location)))
    }

    fun onImport(uri: Uri) = runBusy {
        when (val outcome = importMenusFile(uri.toString())) {
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
}
