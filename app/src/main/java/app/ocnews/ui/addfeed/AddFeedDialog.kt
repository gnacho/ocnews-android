package app.ocnews.ui.addfeed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ocnews.R

@Composable
fun AddFeedDialog(viewModel: AddFeedViewModel, onDismiss: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_feed)) },
        text = {
            Column {
                if (state.discovered.isEmpty()) {
                    OutlinedTextField(
                        value = state.url,
                        onValueChange = viewModel::onUrlChange,
                        label = { Text(stringResource(R.string.feed_or_site_url)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.multiple_feeds),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    state.discovered.forEach { feed ->
                        Text(
                            text = feed.title.ifBlank { feed.url },
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectDiscovered(feed) }
                                .padding(vertical = 8.dp),
                        )
                    }
                }
                state.errorRes?.let { errorRes ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(errorRes, *state.errorArgs.toTypedArray()),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (state.loading) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.working), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            if (state.discovered.isEmpty()) {
                TextButton(onClick = viewModel::add, enabled = !state.loading) {
                    Text(stringResource(R.string.add))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    if (state.done) stringResource(R.string.done)
                    else stringResource(R.string.cancel)
                )
            }
        },
    )
}
