package app.ocnews.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ocnews.BuildConfig
import app.ocnews.R
import app.ocnews.data.settings.FeedListStyle
import app.ocnews.data.settings.SearchScope
import app.ocnews.data.settings.SortOrder
import app.ocnews.data.settings.SwipeAction
import app.ocnews.data.settings.TextSize
import app.ocnews.data.settings.ThemeMode

private val themeOptions = listOf(
    R.string.theme_system to ThemeMode.SYSTEM,
    R.string.theme_light to ThemeMode.LIGHT,
    R.string.theme_dark to ThemeMode.DARK,
)

private val intervalOptions = listOf(
    R.string.interval_never to 0,
    R.string.interval_15 to 15,
    R.string.interval_30 to 30,
    R.string.interval_60 to 60,
    R.string.interval_360 to 360,
)

private val swipeOptions = listOf(
    R.string.swipe_star to SwipeAction.STAR,
    R.string.swipe_read to SwipeAction.READ,
    R.string.swipe_nothing to SwipeAction.NOTHING,
)

private val sortOptions = listOf(
    R.string.sort_newest to SortOrder.NEWEST_FIRST,
    R.string.sort_oldest to SortOrder.OLDEST_FIRST,
)

private val searchOptions = listOf(
    R.string.search_title to SearchScope.TITLE,
    R.string.search_content to SearchScope.CONTENT,
    R.string.search_both to SearchScope.BOTH,
)

private val feedListOptions = listOf(
    R.string.feed_list_thumbnails to FeedListStyle.THUMBNAILS,
    R.string.feed_list_titles to FeedListStyle.TITLES_ONLY,
)

private val textSizeOptions = listOf(
    R.string.text_size_small to TextSize.SMALL,
    R.string.text_size_default to TextSize.DEFAULT,
    R.string.text_size_large to TextSize.LARGE,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onImportOpml: () -> Unit,
    onBack: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(R.string.section_general)
            SwitchRow(R.string.sync_on_start, settings.syncOnStart, viewModel::setSyncOnStart)
            SwitchRow(R.string.only_unread_default, settings.onlyUnread, viewModel::setOnlyUnread)
            ChoiceRow(R.string.swipe_right_action, settings.swipeRight, swipeOptions, viewModel::setSwipeRight)
            ChoiceRow(R.string.swipe_left_action, settings.swipeLeft, swipeOptions, viewModel::setSwipeLeft)
            ChoiceRow(R.string.sort_order, settings.sortOrder, sortOptions, viewModel::setSortOrder)
            ChoiceRow(R.string.search_in, settings.searchIn, searchOptions, viewModel::setSearchIn)

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionHeader(R.string.section_display)
            ChoiceRow(R.string.theme, settings.theme, themeOptions, viewModel::setTheme)
            ChoiceRow(R.string.feed_list_style, settings.feedListStyle, feedListOptions, viewModel::setFeedListStyle)
            ChoiceRow(R.string.text_size, settings.textSize, textSizeOptions, viewModel::setTextSize)

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionHeader(R.string.section_data)
            ChoiceRow(R.string.sync_interval, settings.syncIntervalMinutes, intervalOptions, viewModel::setSyncInterval)
            ActionRow(R.string.clear_cache) {
                android.widget.Toast.makeText(context, R.string.clear_cache, android.widget.Toast.LENGTH_SHORT).show()
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionHeader(R.string.section_about)
            InfoRow(R.string.license, "AGPL-3.0")
            ActionRow(R.string.report_issue) {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/gnacho/ocnews-android/issues"))
                )
            }
            ActionRow(R.string.contribute) {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/gnacho/ocnews-android"))
                )
            }
            InfoRow(R.string.version_label, BuildConfig.VERSION_NAME)

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            ActionRow(R.string.import_opml, icon = true) { onImportOpml() }

            Spacer(Modifier.padding(bottom = 24.dp))
        }
    }
}

@Composable
private fun SectionHeader(titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun SwitchRow(titleRes: Int, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun <T> ChoiceRow(
    titleRes: Int,
    value: T,
    options: List<Pair<Int, T>>,
    onSelect: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val currentLabel = options.firstOrNull { it.second == value }?.first ?: R.string.settings
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { open = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = stringResource(titleRes), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(currentLabel),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (labelRes, optionValue) ->
                DropdownMenuItem(
                    text = { Text(stringResource(labelRes)) },
                    onClick = {
                        open = false
                        onSelect(optionValue)
                    },
                )
            }
        }
    }
}

@Composable
private fun ActionRow(titleRes: Int, icon: Boolean = false, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        if (icon) {
            Icon(Icons.Outlined.FileDownload, contentDescription = null)
            Spacer(Modifier.width(16.dp))
        }
        Text(text = stringResource(titleRes), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun InfoRow(titleRes: Int, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = stringResource(titleRes), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
