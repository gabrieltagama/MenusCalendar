package com.gabrieltagama.menuplanner.feature.settings.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrieltagama.menuplanner.core.domain.model.ThemeMode
import com.gabrieltagama.menuplanner.core.ui.theme.MenuPlannerTheme
import com.gabrieltagama.menuplanner.feature.settings.R

/**
 * Settings tab: stateful route bound to [SettingsViewModel] and a stateless screen grouped in
 * sections; for now "Apariencia" with the theme choice (system, light or dark). Bottom insets are
 * left to the app shell, which draws the NavigationBar below this screen.
 */
@Composable
fun SettingsScreenRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    SettingsScreen(themeMode = themeMode, onThemeModeSelect = viewModel::onThemeModeSelect)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(themeMode: ThemeMode, onThemeModeSelect: (ThemeMode) -> Unit) = Scaffold(
    contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
    topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }
) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
        SectionHeader(R.string.settings_section_appearance)
        ThemeModeOptions(selected = themeMode, onSelect = onThemeModeSelect)
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
    SettingsScreen(themeMode = ThemeMode.LIGHT, onThemeModeSelect = {})
}
