package com.gabrieltagama.menuplanner.feature.onboarding.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrieltagama.menuplanner.core.cloud.auth.rememberGoogleDriveConnect
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.ui.text.message
import com.gabrieltagama.menuplanner.core.ui.theme.MenuPlannerTheme
import com.gabrieltagama.menuplanner.feature.onboarding.R

/**
 * First-start welcome UI: [WelcomeScreenRoute] wires [WelcomeViewModel] and the Google consent
 * flow, while [WelcomeScreen] is a stateless rendering of [WelcomeUiState] with the choice between
 * linking Google for the recipe backup or using the app only on this device.
 */
@Composable
fun WelcomeScreenRoute(viewModel: WelcomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val connectGoogle = rememberGoogleDriveConnect(onResult = viewModel::onGoogleAuthorization)
    WelcomeScreen(
        state = state,
        onConnectGoogle = {
            viewModel.onConnectStarted()
            connectGoogle()
        },
        onContinueLocal = viewModel::onContinueWithoutAccount
    )
}

@Composable
internal fun WelcomeScreen(
    state: WelcomeUiState,
    onConnectGoogle: () -> Unit,
    onContinueLocal: () -> Unit,
    modifier: Modifier = Modifier
) = Scaffold(modifier = modifier) { innerPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.RestaurantMenu,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.welcome_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.welcome_privacy),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        if (state.isConnecting) ConnectingIndicator()
        else WelcomeActions(onConnectGoogle = onConnectGoogle, onContinueLocal = onContinueLocal)
        state.message?.let { WelcomeMessageText(it) }
    }
}

@Composable
private fun ConnectingIndicator() = Column(horizontalAlignment = Alignment.CenterHorizontally) {
    CircularProgressIndicator()
    Spacer(modifier = Modifier.height(12.dp))
    Text(text = stringResource(R.string.welcome_connecting), style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun WelcomeActions(onConnectGoogle: () -> Unit, onContinueLocal: () -> Unit) = Column {
    Button(onClick = onConnectGoogle, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.welcome_connect_google))
    }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(onClick = onContinueLocal, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.welcome_continue_local))
    }
}

@Composable
private fun WelcomeMessageText(message: WelcomeMessage) = Text(
    text = when (message) {
        WelcomeMessage.ConnectionCancelled -> stringResource(R.string.welcome_connect_cancelled)
        is WelcomeMessage.Failed -> message.error.message()
    },
    color = MaterialTheme.colorScheme.error,
    style = MaterialTheme.typography.bodyMedium,
    textAlign = TextAlign.Center,
    modifier = Modifier.padding(top = 16.dp)
)

@Preview(showBackground = true)
@Composable
private fun WelcomeScreenPreview() = MenuPlannerTheme {
    WelcomeScreen(state = WelcomeUiState(), onConnectGoogle = {}, onContinueLocal = {})
}

@Preview(showBackground = true)
@Composable
private fun WelcomeScreenErrorPreview() = MenuPlannerTheme {
    WelcomeScreen(
        state = WelcomeUiState(message = WelcomeMessage.Failed(DomainError.CloudUnavailable)),
        onConnectGoogle = {},
        onContinueLocal = {}
    )
}
