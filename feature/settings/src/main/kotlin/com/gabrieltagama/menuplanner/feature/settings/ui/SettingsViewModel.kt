package com.gabrieltagama.menuplanner.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.CloudAccount
import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import com.gabrieltagama.menuplanner.core.domain.model.ThemeMode
import com.gabrieltagama.menuplanner.core.domain.usecase.backup.BackUpRecipesUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.backup.ConnectCloudAccountUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.backup.DisconnectCloudAccountUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.backup.ObserveCloudAccountUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.backup.RestoreRecipesUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.settings.ObserveThemeModeUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.settings.SetThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Settings screen state: the chosen theme mode and the optional Google recipe backup, which can
 * be linked, run or restored on demand and unlinked. Backup results surface as a nullable
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
    observeCloudAccount: ObserveCloudAccountUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val connectCloudAccount: ConnectCloudAccountUseCase,
    private val backUpRecipes: BackUpRecipesUseCase,
    private val restoreRecipes: RestoreRecipesUseCase,
    private val disconnectCloudAccount: DisconnectCloudAccountUseCase
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = observeThemeMode()

    val cloudAccount: StateFlow<CloudAccount> = observeCloudAccount()

    private val _backupState = MutableStateFlow(BackupUiState())
    val backupState: StateFlow<BackupUiState> = _backupState.asStateFlow()

    fun onThemeModeSelect(mode: ThemeMode) {
        viewModelScope.launch { setThemeMode(mode) }
    }

    fun onConnectStarted() = _backupState.update { it.copy(isBusy = true, message = null) }

    fun onGoogleAuthorization(accessToken: String?) {
        if (accessToken == null) showMessage(BackupMessage.ConnectionCancelled)
        else runBusy { connectCloudAccount(accessToken).toMessage(BackupMessage::Restored) }
    }

    fun onBackUpNow() = runBusy { backUpRecipes(onlyIfChanged = false).toMessage { BackupMessage.BackedUp } }

    fun onRestore() = runBusy { restoreRecipes().toMessage(BackupMessage::Restored) }

    fun onDisconnect() = runBusy {
        disconnectCloudAccount()
        BackupMessage.Disconnected
    }

    fun onMessageShown() = _backupState.update { it.copy(message = null) }

    private fun runBusy(action: suspend () -> BackupMessage) {
        viewModelScope.launch {
            _backupState.update { it.copy(isBusy = true, message = null) }
            showMessage(action())
        }
    }

    private fun showMessage(message: BackupMessage) = _backupState.update { it.copy(isBusy = false, message = message) }

    private fun <T> Outcome<T>.toMessage(onSuccess: (T) -> BackupMessage): BackupMessage = when (this) {
        is Outcome.Success -> onSuccess(value)
        is Outcome.Failure -> BackupMessage.Failed(error)
    }
}
