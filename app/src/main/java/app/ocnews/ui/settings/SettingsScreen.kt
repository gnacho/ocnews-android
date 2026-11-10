package app.ocnews.ui.settings

import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ocnews.BuildConfig
import app.ocnews.R
import app.ocnews.ui.common.AppMenu
import app.ocnews.data.settings.FeedListStyle
import app.ocnews.data.settings.SearchScope
import app.ocnews.data.settings.ImageCacheMode
import app.ocnews.data.settings.SortOrder
import app.ocnews.data.settings.SwipeAction
import app.ocnews.data.settings.TextSize
import app.ocnews.data.settings.ThemeMode
import app.ocnews.ui.addfeed.AddFeedDialog
import app.ocnews.ui.addfeed.AddFeedViewModel

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

private val imageCacheOptions = listOf(
    R.string.image_cache_never to ImageCacheMode.NEVER,
    R.string.image_cache_wifi to ImageCacheMode.WIFI_ONLY,
    R.string.image_cache_wifi_mobile to ImageCacheMode.WIFI_AND_MOBILE,
    R.string.image_cache_ask to ImageCacheMode.ASK,
)

private val maxCacheSizeOptions = listOf(250, 500, 750, 1000, 2000, 5000, 10000)

private fun cacheSizeLabel(mb: Int): String =
    if (mb >= 1000) "${mb / 1000} GB" else "$mb MB"

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
    onCreateFolder: (String) -> Unit,
    addFeedViewModel: AddFeedViewModel,
    onClearCache: () -> Unit,
    serverUrl: String,
    username: String,
    onLogout: () -> Unit,
    onBack: () -> Unit,
) {
    // Back returns to the list instead of closing the app.
    BackHandler(onBack = onBack)
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAddFeed by remember { mutableStateOf(false) }
    var showNewFolder by remember { mutableStateOf(false) }
    var folderName by remember { mutableStateOf("") }
    var showLogout by remember { mutableStateOf(false) }

    if (showLogout) {
        AlertDialog(
            onDismissRequest = { showLogout = false },
            title = { Text(stringResource(R.string.logout_confirm_title)) },
            text = { Text(stringResource(R.string.logout_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogout = false
                    onLogout()
                }) { Text(stringResource(R.string.log_out)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogout = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showAddFeed) {
        AddFeedDialog(viewModel = addFeedViewModel, onDismiss = { showAddFeed = false })
    }

    if (showNewFolder) {
        AlertDialog(
            onDismissRequest = { showNewFolder = false },
            title = { Text(stringResource(R.string.new_folder)) },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.folder_name)) },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (folderName.isNotBlank()) {
                        onCreateFolder(folderName.trim())
                        folderName = ""
                        showNewFolder = false
                    }
                }) { Text(stringResource(R.string.add)) }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolder = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

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
            ActionRow(R.string.new_feed) { showAddFeed = true }
            ActionRow(R.string.new_folder) {
                folderName = ""
                showNewFolder = true
            }
            ActionRow(R.string.import_opml, icon = true) { onImportOpml() }
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
            SwitchRow(R.string.oled_background, settings.oledBackground, viewModel::setOledBackground)
            SwitchRow(
                R.string.hide_related_sections,
                settings.hideRelatedSections,
                viewModel::setHideRelatedSections,
            )
            SwitchRow(
                R.string.volume_buttons,
                settings.navigateWithVolumeButtons,
                viewModel::setNavigateWithVolumeButtons,
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionHeader(R.string.section_data)
            ChoiceRow(R.string.sync_interval, settings.syncIntervalMinutes, intervalOptions, viewModel::setSyncInterval)
            ChoiceRow(
                R.string.image_cache_offline,
                settings.imageCacheMode,
                imageCacheOptions,
                viewModel::setImageCacheMode,
            )
            ChoiceRowLabels(
                R.string.max_cache_size,
                settings.maxCacheSizeMb,
                maxCacheSizeOptions.map { mb -> cacheSizeLabel(mb) to mb },
                viewModel::setMaxCacheSizeMb,
            )
            ActionRow(R.string.clear_cache) { onClearCache() }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionHeader(R.string.section_account)
            InfoRow(R.string.server_url_label, serverUrl)
            InfoRow(R.string.username_label, username)
            ActionRow(R.string.log_out) { showLogout = true }

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
    val labels = options.map { stringResource(it.first) to it.second }
    ChoiceRowLabels(titleRes, value, labels, onSelect)
}

// Same row with labels that are already text (for values such as cache sizes).
@Composable
private fun <T> ChoiceRowLabels(
    titleRes: Int,
    value: T,
    options: List<Pair<String, T>>,
    onSelect: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    var rowBottom by remember { mutableIntStateOf(0) }
    val currentLabel = options.firstOrNull { it.second == value }?.first
        ?: stringResource(R.string.settings)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                rowBottom = (coords.positionInRoot().y + coords.size.height).toInt()
            }
            .clickable { open = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = stringResource(titleRes), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = currentLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AppMenu(
            expanded = open,
            onDismissRequest = { open = false },
            topOffset = with(LocalDensity.current) { rowBottom.toDp() },
        ) {
            options.forEach { (label, optionValue) ->
                DropdownMenuItem(
                    text = { Text(label) },
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
