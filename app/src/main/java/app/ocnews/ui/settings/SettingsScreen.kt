package app.ocnews.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ocnews.data.settings.ThemeMode

private val themeOptions = listOf(
    ThemeMode.SYSTEM to "System",
    ThemeMode.LIGHT to "Light",
    ThemeMode.DARK to "Dark",
)

private val intervalOptions = listOf(
    0 to "Never",
    15 to "15 minutes",
    30 to "30 minutes",
    60 to "1 hour",
    360 to "6 hours",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        Text(
            text = "Theme",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        themeOptions.forEach { (mode, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setTheme(mode) }
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                RadioButton(selected = settings.theme == mode, onClick = { viewModel.setTheme(mode) })
                Spacer(Modifier.width(8.dp))
                Text(label)
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        Text(
            text = "Sync interval",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        intervalOptions.forEach { (minutes, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setSyncInterval(minutes) }
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                RadioButton(
                    selected = settings.syncIntervalMinutes == minutes,
                    onClick = { viewModel.setSyncInterval(minutes) },
                )
                Spacer(Modifier.width(8.dp))
                Text(label)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
