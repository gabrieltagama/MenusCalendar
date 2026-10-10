package com.gabrieltagama.menuplanner.feature.settings.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrieltagama.menuplanner.core.cloud.auth.rememberGoogleDriveConnect
import com.gabrieltagama.menuplanner.core.domain.model.CloudAccount
import com.gabrieltagama.menuplanner.core.domain.model.ThemeMode
import com.gabrieltagama.menuplanner.core.ui.text.message
import com.gabrieltagama.menuplanner.core.ui.theme.MenuPlannerTheme
import com.gabrieltagama.menuplanner.feature.settings.R

/**
 * Settings tab: stateful route bound to [SettingsViewModel] and the Google consent flow, and a
 * stateless screen grouped in sections: "Apariencia" with the theme choice (system, light or dark)
 * and the optional Google Drive recipe backup. Bottom insets are left to the app shell, which
 * draws the NavigationBar below this screen.
 */
@Composable
fun SettingsScreenRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val cloudAccount by viewModel.cloudAccount.collectAsStateWithLifecycle()
    val backupState by viewModel.backupState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val connectGoogle = rememberGoogleDriveConnect(onResult = viewModel::onGoogleAuthorization)

    val messageText = backupState.message?.text()
    LaunchedEffect(backupState.message) {
        if (messageText == null) return@LaunchedEffect
        snackbarHostState.showSnackbar(messageText)
        viewModel.onMessageShown()
    }

    SettingsScreen(
        themeMode = themeMode,
        onThemeModeSelect = viewModel::onThemeModeSelect,
        cloudAccount = cloudAccount,
        isBackupBusy = backupState.isBusy,
        backupActions = BackupActions(
            onConnect = {
                viewModel.onConnectStarted()
                connectGoogle()
            },
            onBackUpNow = viewModel::onBackUpNow,
            onRestore = viewModel::onRestore,
            onDisconnect = viewModel::onDisconnect
        ),
        snackbarHostState = snackbarHostState
    )
}

class BackupActions(
    val onConnect: () -> Unit,
    val onBackUpNow: () -> Unit,
    val onRestore: () -> Unit,
    val onDisconnect: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeSelect: (ThemeMode) -> Unit,
    cloudAccount: CloudAccount,
    isBackupBusy: Boolean,
    backupActions: BackupActions,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) = Scaffold(
    contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
    topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
    snackbarHost = { SnackbarHost(snackbarHostState) }
) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
        SectionHeader(R.string.settings_section_appearance)
        ThemeModeOptions(selected = themeMode, onSelect = onThemeModeSelect)
        SectionHeader(R.string.settings_section_backup)
        BackupOptions(account = cloudAccount, isBusy = isBackupBusy, actions = backupActions)
    }
}

@Composable
private fun SectionHeader(@StringRes titleRes: Int) = Text(
    text = stringResource(titleRes),
    style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
)

@Composable
private fun ThemeModeOptions(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) = Column(modifier = Modifier.selectableGroup()) {
    Text(
        text = stringResource(R.string.settings_theme),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
    ThemeMode.entries.forEach { mode ->
        ThemeModeOption(mode = mode, isSelected = mode == selected, onSelect = { onSelect(mode) })
    }
}

@Composable
private fun BackupOptions(account: CloudAccount, isBusy: Boolean, actions: BackupActions) = Column {
    if (isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
    when (account) {
        CloudAccount.Disconnected -> BackupItem(
            icon = Icons.Filled.Cloud,
            title = stringResource(R.string.settings_backup_connect),
            hint = stringResource(R.string.settings_backup_connect_hint),
            enabled = !isBusy,
            onClick = actions.onConnect
        )
        is CloudAccount.Connected -> ConnectedBackupOptions(email = account.email, isBusy = isBusy, actions = actions)
    }
}

@Composable
private fun ConnectedBackupOptions(email: String, isBusy: Boolean, actions: BackupActions) = Column {
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_backup_connected, email)) },
        supportingContent = { Text(stringResource(R.string.settings_backup_connected_hint)) },
        leadingContent = { Icon(Icons.Filled.CloudDone, contentDescription = null) }
    )
    BackupItem(Icons.Filled.Backup, stringResource(R.string.settings_backup_now), hint = null, enabled = !isBusy, onClick = actions.onBackUpNow)
    BackupItem(Icons.Filled.Restore, stringResource(R.string.settings_backup_restore), stringResource(R.string.settings_backup_restore_hint), enabled = !isBusy, onClick = actions.onRestore)
    BackupItem(Icons.Filled.CloudOff, stringResource(R.string.settings_backup_disconnect), stringResource(R.string.settings_backup_disconnect_hint), enabled = !isBusy, onClick = actions.onDisconnect)
}

@Composable
private fun BackupItem(icon: ImageVector, title: String, hint: String?, enabled: Boolean, onClick: () -> Unit) = ListItem(
    headlineContent = { Text(title) },
    supportingContent = if (hint == null) null else ({ Text(hint) }),
    leadingContent = { Icon(icon, contentDescription = null) },
    modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
)

@Composable
private fun BackupMessage.text(): String = when (this) {
    BackupMessage.BackedUp -> stringResource(R.string.settings_backup_done)
    is BackupMessage.Restored -> stringResource(R.string.settings_backup_restored, summary.dishesAdded, summary.dishesUpdated)
    BackupMessage.Disconnected -> stringResource(R.string.settings_backup_disconnected)
    BackupMessage.ConnectionCancelled -> stringResource(R.string.settings_backup_cancelled)
    is BackupMessage.Failed -> error.message()
}

@Composable
private fun ThemeModeOption(mode: ThemeMode, isSelected: Boolean, onSelect: () -> Unit) = ListItem(
    headlineContent = { Text(stringResource(mode.labelRes)) },
    supportingContent = { Text(stringResource(mode.hintRes)) },
    leadingContent = { RadioButton(selected = isSelected, onClick = null) },
    modifier = Modifier.selectable(selected = isSelected, onClick = onSelect, role = Role.RadioButton)
)

private val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    }

private val ThemeMode.hintRes: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system_hint
        ThemeMode.LIGHT -> R.string.settings_theme_light_hint
        ThemeMode.DARK -> R.string.settings_theme_dark_hint
    }

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() = MenuPlannerTheme {
    SettingsScreen(
        themeMode = ThemeMode.LIGHT,
        onThemeModeSelect = {},
        cloudAccount = CloudAccount.Connected("usuario@gmail.com"),
        isBackupBusy = false,
        backupActions = BackupActions(onConnect = {}, onBackUpNow = {}, onRestore = {}, onDisconnect = {})
    )
}
