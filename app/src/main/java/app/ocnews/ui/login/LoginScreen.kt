package app.ocnews.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.ocnews.BuildConfig
import app.ocnews.R

@Composable
fun LoginScreen(
    state: LoginUiState,
    onServerUrlChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onLogin: () -> Unit,
    onBrowserLogin: () -> Unit,
    onOpenBrowser: (String) -> Unit,
    onCopyToClipboard: (String) -> Unit,
    onBrowserLoginCancel: () -> Unit,
    onPickNextcloudAccount: () -> Unit,
    onRunDiagnostics: () -> Unit,
    onCopyDiagnostics: (List<String>) -> Unit,
) {
    var provider by remember { mutableStateOf(ServerProvider.OPENCLOUD) }
    val passwordLabel = when (provider) {
        ServerProvider.OPENCLOUD -> R.string.app_token
        ServerProvider.NEXTCLOUD -> R.string.app_password
    }
    val passwordHint = when (provider) {
        ServerProvider.OPENCLOUD -> R.string.app_token_hint
        ServerProvider.NEXTCLOUD -> R.string.app_password_hint
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.server_type),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = provider == ServerProvider.NEXTCLOUD,
                onClick = { provider = ServerProvider.NEXTCLOUD },
                label = { Text(stringResource(R.string.provider_nextcloud)) },
            )
            FilterChip(
                selected = provider == ServerProvider.OPENCLOUD,
                onClick = { provider = ServerProvider.OPENCLOUD },
                label = { Text(stringResource(R.string.provider_opencloud)) },
            )
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = state.serverUrl,
            onValueChange = onServerUrlChange,
            label = { Text(stringResource(R.string.server_url)) },
            placeholder = { Text(stringResource(R.string.server_url_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.username,
            onValueChange = onUsernameChange,
            label = { Text(stringResource(R.string.username)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.token,
            onValueChange = onTokenChange,
            label = { Text(stringResource(passwordLabel)) },
            supportingText = { Text(stringResource(passwordHint)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.errorRes != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(state.errorRes, *state.errorArgs.toTypedArray()),
                color = MaterialTheme.colorScheme.error,
            )
        }
        state.detail?.let { detail ->
            Spacer(Modifier.height(4.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))
        // Nextcloud: reuse the account already configured in the Nextcloud app.
        if (provider == ServerProvider.NEXTCLOUD) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onPickNextcloudAccount,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.use_nextcloud_account))
            }
        }
        // Nextcloud: authorise in the browser instead of pasting an app password.
        if (provider == ServerProvider.NEXTCLOUD) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onBrowserLogin,
                enabled = !state.loading && !state.browserLoginWaiting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.sign_in_with_browser))
            }
            if (state.browserLoginWaiting) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (state.browserLoginUrl != null) {
                        stringResource(R.string.waiting_browser)
                    } else {
                        stringResource(R.string.checking_server)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Some phones block launching the browser from another app, so
                // leave both a retry and a way to take the link elsewhere.
                val link = state.browserLoginUrl
                if (link != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onOpenBrowser(link) }) {
                            Text(stringResource(R.string.open_browser_again))
                        }
                        TextButton(onClick = { onCopyToClipboard(link) }) {
                            Text(stringResource(R.string.copy_link))
                        }
                    }
                }
            }
            // Authorise inside the app: launching another app may be blocked.
            state.browserLoginUrl?.let { url ->
                NextcloudLoginSheet(
                    url = url,
                    onClose = onBrowserLoginCancel,
                    onOpenBrowser = onOpenBrowser,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onLogin,
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (state.loading) stringResource(R.string.signing_in)
                else stringResource(R.string.sign_in)
            )
        }
        TextButton(
            onClick = onRunDiagnostics,
            enabled = !state.diagnosticRunning,
        ) {
            Text(
                if (state.diagnosticRunning) stringResource(R.string.diagnostic_running)
                else stringResource(R.string.diagnostic)
            )
        }
        state.diagnostic?.let { report ->
            Text(
                text = report.joinToString("\n"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
            )
            TextButton(onClick = { onCopyDiagnostics(report) }) {
                Text(stringResource(R.string.diagnostic_copy))
            }
        }
        Text(
            text = "ocnews ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
    }
}

// Only the wording changes: both servers authenticate with Basic user + app password.
enum class ServerProvider { NEXTCLOUD, OPENCLOUD }
