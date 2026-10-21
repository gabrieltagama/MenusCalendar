package com.gabrieltagama.menuplanner.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.fold
import com.gabrieltagama.menuplanner.core.domain.model.CloudAccount
import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import com.gabrieltagama.menuplanner.core.domain.model.RepeatWindow
import com.gabrieltagama.menuplanner.core.domain.model.ThemeMode
import com.gabrieltagama.menuplanner.core.domain.usecase.backup.BackUpUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.backup.ConnectCloudAccountUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.backup.DisconnectCloudAccountUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.backup.ObserveCloudAccountUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.backup.RestoreBackupUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.settings.ObserveRepeatWindowUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.settings.ObserveThemeModeUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.settings.SetRepeatWindowUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.settings.SetThemeModeUseCase
import com.gabrieltagama.menuplanner.core.ui.state.BusyRunner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Settings screen state: the chosen theme mode, the days without repeating a dish of the autofill
 * and the optional Google backup of recipes and calendar, which can
 * be linked, run or restored on demand and unlinked. Only one backup operation runs at a time and
 * the busy flag is always released, even when the operation fails or is cancelled. Backup results surface as a nullable
 * [BackupMessage] in [BackupUiState].
 */
data class BackupUiState(
    val isBusy: Boolean = false,
    val message: BackupMessage? = null
)

sealed interface BackupMessage {
    data object BackedUp : BackupMessage
    data class Restored(val summary: ImportSummary) : BackupMessage
    data object Disconnected : BackupMessage
    data object ConnectionCancelled : BackupMessage
    data class Failed(val error: DomainError) : BackupMessage
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeThemeMode: ObserveThemeModeUseCase,
    observeRepeatWindow: ObserveRepeatWindowUseCase,
    observeCloudAccount: ObserveCloudAccountUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val setRepeatWindow: SetRepeatWindowUseCase,
    private val connectCloudAccount: ConnectCloudAccountUseCase,
    private val backUp: BackUpUseCase,
    private val restoreBackup: RestoreBackupUseCase,
    private val disconnectCloudAccount: DisconnectCloudAccountUseCase
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> =
        observeThemeMode().stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ThemeMode.SYSTEM)

    val repeatWindow: StateFlow<RepeatWindow> =
        observeRepeatWindow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), RepeatWindow.DEFAULT)

    val cloudAccount: StateFlow<CloudAccount> =
        observeCloudAccount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), CloudAccount.Disconnected)

    private val _backupState = MutableStateFlow(BackupUiState())
    val backupState: StateFlow<BackupUiState> = _backupState.asStateFlow()

    private val busy = BusyRunner(viewModelScope) { isBusy -> _backupState.update { it.copy(isBusy = isBusy, message = if (isBusy) null else it.message) } }

    fun onThemeModeSelect(mode: ThemeMode) {
        viewModelScope.launch { setThemeMode(mode) }
    }

    fun onRepeatWindowChange(days: Int) {
        viewModelScope.launch { setRepeatWindow(days) }
    }

    fun onConnectStarted() = _backupState.update { it.copy(isBusy = true, message = null) }

    fun onGoogleAuthorization(accessToken: String?) {
        if (accessToken == null) showMessage(BackupMessage.ConnectionCancelled)
        else runBusy { connectCloudAccount(accessToken).fold(BackupMessage::Restored, BackupMessage::Failed) }
    }

    fun onBackUpNow() = runBusy { backUp(onlyIfChanged = false).fold({ BackupMessage.BackedUp }, BackupMessage::Failed) }

    fun onRestore() = runBusy { restoreBackup().fold(BackupMessage::Restored, BackupMessage::Failed) }

    fun onDisconnect() = runBusy {
        disconnectCloudAccount()
        BackupMessage.Disconnected
    }

    fun onMessageShown() = _backupState.update { it.copy(message = null) }

    private fun runBusy(action: suspend () -> BackupMessage) = busy.run { showMessage(action()) }

    private fun showMessage(message: BackupMessage) = _backupState.update { it.copy(isBusy = false, message = message) }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
