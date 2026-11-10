package app.ocnews.ui.list

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.RadioButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ocnews.R
import app.ocnews.ui.common.AppMenu
import app.ocnews.data.local.FeedEntity
import app.ocnews.data.local.FolderEntity
import app.ocnews.data.local.ItemEntity
import app.ocnews.data.local.ItemSummary
import app.ocnews.data.settings.AppSettings
import app.ocnews.data.settings.FeedListStyle
import app.ocnews.data.settings.SwipeAction
import app.ocnews.ui.addfeed.AddFeedDialog
import app.ocnews.ui.addfeed.AddFeedViewModel
import app.ocnews.util.firstImageUrl
import app.ocnews.util.isRelatedHeading
import app.ocnews.util.isCardBlock
import app.ocnews.util.stripCardLinkRuns
import app.ocnews.util.isRelatedLink
import app.ocnews.util.format
import app.ocnews.util.relativeTime
import app.ocnews.util.unpackImageUrl
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val StarGold = Color(0xFFFFB300)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    folders: List<FolderEntity>,
    feeds: List<FeedEntity>,
    items: List<ItemSummary>,
    scope: Scope,
    onlyUnread: Boolean,
    syncing: Boolean,
    unreadCount: Int,
    starredCount: Int,
    serverUrl: String,
    settings: AppSettings,
    isDark: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    fullContent: Map<Long, String>,
    itemDetails: Map<Long, ItemEntity>,
    download: DownloadProgress?,
    finished: DownloadFinished?,
    pendingImageDownload: Boolean,
    onConfirmImageDownload: () -> Unit,
    onCancelImageDownload: () -> Unit,
    navigateWithVolume: Boolean,
    hideRelated: Boolean,
    onDownloadArticles: () -> Unit,
    onDownloadImages: () -> Unit,
    onFinishedShown: () -> Unit,
    onAutoRead: (Long) -> Unit,
    onReaderClosed: () -> Unit,
    fullTextFeedIds: Set<Long>,
    onSaveFeedSettings: (Long, Long?, Boolean) -> Unit,
    onFetchFull: (Long) -> Unit,
    onLoadDetail: (Long) -> Unit,
    addFeedViewModel: AddFeedViewModel,
    onSelectScope: (Scope) -> Unit,
    onToggleOnlyUnread: () -> Unit,
    onToggleRead: (Long, Boolean) -> Unit,
    onToggleStarred: (Long, Boolean) -> Unit,
    onSync: () -> Unit,
    onLogout: () -> Unit,
    onMarkAllRead: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    // Snapshot of the list taken when the reader opens: toggling read/starred
    // changes the filtered list behind, and the reader must not jump or close.
    var readerItems by remember { mutableStateOf<List<ItemSummary>?>(null) }
    var readerOpen by remember { mutableStateOf(false) }
    var feedSettingsFor by remember { mutableStateOf<FeedEntity?>(null) }
    var showAddFeed by remember { mutableStateOf(false) }
    var searchActive by remember { mutableStateOf(false) }
    var profileMenuOpen by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    LaunchedEffect(finished) {
        val result = finished ?: return@LaunchedEffect
        val text = when {
            result.blocked == DownloadBlocked.DISABLED -> context.getString(R.string.download_disabled)
            result.blocked == DownloadBlocked.WIFI_ONLY -> context.getString(R.string.download_wifi_blocked)
            result.count == 0 -> context.getString(R.string.nothing_to_download)
            result.kind == DownloadKind.ARTICLES ->
                context.resources.getQuantityString(R.plurals.downloaded_articles, result.count, result.count)
            else ->
                context.resources.getQuantityString(R.plurals.downloaded_images, result.count, result.count)
        }
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        onFinishedShown()
    }

    if (showAddFeed) {
        AddFeedDialog(
            viewModel = addFeedViewModel,
            onDismiss = { showAddFeed = false },
        )
    }

    feedSettingsFor?.let { feed ->
        FeedSettingsDialog(
            feed = feed,
            folders = folders,
            fullText = fullTextFeedIds.contains(feed.id),
            onSave = { folderId, fullText ->
                onSaveFeedSettings(feed.id, folderId, fullText)
                feedSettingsFor = null
            },
            onDismiss = { feedSettingsFor = null },
        )
    }

    if (pendingImageDownload) {
        AlertDialog(
            onDismissRequest = onCancelImageDownload,
            title = { Text(stringResource(R.string.ask_images_title)) },
            text = { Text(stringResource(R.string.ask_images_message)) },
            confirmButton = {
                TextButton(onClick = onConfirmImageDownload) {
                    Text(stringResource(R.string.download))
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelImageDownload) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // Once the reader has slid away, forget its data and reset the session.
    LaunchedEffect(readerOpen) {
        if (!readerOpen && readerItems != null) {
            delay(320)
            readerItems = null
            selectedIndex = null
            onReaderClosed()
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text(stringResource(R.string.logout_confirm_title)) },
            text = { Text(stringResource(R.string.logout_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    onLogout()
                }) { Text(stringResource(R.string.log_out)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(drawerWidth(LocalConfiguration.current.screenWidthDp))) {
                DrawerContent(
                    folders = folders,
                    feeds = feeds,
                    scope = scope,
                    unreadCount = unreadCount,
                    starredCount = starredCount,
                    onFeedSettings = { feed ->
                        feedSettingsFor = feed
                        coroutineScope.launch { drawerState.close() }
                    },
                    onSelectScope = { s ->
                        onSelectScope(s)
                        coroutineScope.launch { drawerState.close() }
                    },
                    onOpenSettings = onOpenSettings,
                )
            }
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            if (searchActive) {
                                TextField(
                                    value = searchQuery,
                                    onValueChange = onSearchQueryChange,
                                    placeholder = { Text(stringResource(R.string.search_hint)) },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else if (scope == Scope.All) {
                                Text(
                                    text = if (onlyUnread) stringResource(R.string.unread_articles)
                                    else stringResource(R.string.all_articles),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable { onToggleOnlyUnread() },
                                )
                            } else {
                                Text(
                                    scopeLabel(scope, folders),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        },
                        navigationIcon = {
                            if (searchActive) {
                                IconButton(onClick = {
                                    onSearchQueryChange("")
                                    searchActive = false
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close))
                                }
                            } else {
                                IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                    Text("☰")
                                }
                            }
                        },
                        actions = {
                            if (searchActive) {
                                // Search mode: no other actions.
                            } else {
                                IconButton(onClick = { searchActive = true }) {
                                    Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search))
                                }
                                var profileMenuY by remember { mutableIntStateOf(0) }
                                Box(
                                    Modifier.onGloballyPositioned { coords ->
                                        profileMenuY = (coords.positionInRoot().y + coords.size.height).toInt()
                                    },
                                ) {
                                    IconButton(onClick = { profileMenuOpen = true }) {
                                        Icon(Icons.Filled.MoreVert, contentDescription = null)
                                    }
                                    AppMenu(
                                        expanded = profileMenuOpen,
                                        onDismissRequest = { profileMenuOpen = false },
                                        topOffset = with(LocalDensity.current) { profileMenuY.toDp() },
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.add_feed)) },
                                            onClick = {
                                                profileMenuOpen = false
                                                showAddFeed = true
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.refresh)) },
                                            onClick = {
                                                profileMenuOpen = false
                                                onSync()
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.download_articles)) },
                                            onClick = {
                                                profileMenuOpen = false
                                                onDownloadArticles()
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.download_images)) },
                                            onClick = {
                                                profileMenuOpen = false
                                                onDownloadImages()
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.mark_all_read)) },
                                            onClick = {
                                                profileMenuOpen = false
                                                onMarkAllRead()
                                            },
                                        )
                                        HorizontalDivider()
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.log_out)) },
                                            onClick = {
                                                profileMenuOpen = false
                                                confirmLogout = true
                                            },
                                        )
                                    }
                                }
                            }
                        },
                    )
                },
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    Column(Modifier.fillMaxSize()) {
                        download?.let { progress ->
                            Column(Modifier.fillMaxWidth()) {
                                LinearProgressIndicator(
                                progress = {
                                    if (progress.total == 0) 0f
                                    else progress.done.toFloat() / progress.total
                                },
                                modifier = Modifier.fillMaxWidth(),
                                )
                                Text(
                                    text = stringResource(
                                        R.string.download_progress,
                                        progress.done,
                                        progress.total,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                )
                            }
                        }
                    PullToRefreshBox(
                        isRefreshing = syncing,
                        onRefresh = onSync,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        ArticleList(
                            items = items,
                            feeds = feeds,
                            swipeRight = settings.swipeRight,
                            swipeLeft = settings.swipeLeft,
                            feedListStyle = settings.feedListStyle,
                            onToggleRead = onToggleRead,
                            onToggleStarred = onToggleStarred,
                            onItemClick = { item ->
                                readerItems = items
                                readerOpen = true
                                selectedIndex = items.indexOfFirst { it.id == item.id }.takeIf { it >= 0 }
                            },
                        )
                    }
                    }
                }
            }
            val readerList = readerItems
            val readerIndex = selectedIndex ?: 0
            val readerItem = readerList?.getOrNull(readerIndex)
            // Slides in from the right and back out, so leaving an article is not a jump.
            AnimatedVisibility(
                visible = readerOpen && readerItem != null,
                enter = slideInHorizontally(animationSpec = tween(300)) { it },
                exit = slideOutHorizontally(animationSpec = tween(300)) { it },
            ) {
                if (readerItem == null || readerList == null) return@AnimatedVisibility
                Reader(
                    items = readerList,
                    initialIndex = readerIndex,
                    feeds = feeds,
                    serverUrl = serverUrl,
                    isDark = isDark,
                    fullContent = fullContent,
                    itemDetails = itemDetails,
                    navigateWithVolume = navigateWithVolume,
                    hideRelated = hideRelated,
                    fullTextFeedIds = fullTextFeedIds,
                    onEditFeed = { feedId ->
                        feeds.firstOrNull { it.id == feedId }?.let { feedSettingsFor = it }
                    },
                    onAutoRead = onAutoRead,
                    onLoadDetail = onLoadDetail,
                    onFetchFull = onFetchFull,
                    onBack = { readerOpen = false },
                    onToggleRead = onToggleRead,
                    onToggleStarred = onToggleStarred,
            )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun FeedSettingsDialog(
    feed: FeedEntity,
    folders: List<FolderEntity>,
    fullText: Boolean,
    onSave: (Long?, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var folderId by remember { mutableStateOf(feed.folderId) }
    var fullTextEnabled by remember { mutableStateOf(fullText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(feed.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.feed_folder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FolderOption(
                    label = stringResource(R.string.no_folder),
                    selected = folderId == null,
                    onClick = { folderId = null },
                )
                folders.forEach { folder ->
                    FolderOption(
                        label = folder.name,
                        selected = folderId == folder.id,
                        onClick = { folderId = folder.id },
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.fetch_full_per_feed),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(R.string.fetch_full_per_feed_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = fullTextEnabled, onCheckedChange = { fullTextEnabled = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(folderId, fullTextEnabled) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun FolderOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DrawerContent(
    folders: List<FolderEntity>,
    feeds: List<FeedEntity>,
    scope: Scope,
    unreadCount: Int,
    starredCount: Int,
    onSelectScope: (Scope) -> Unit,
    onFeedSettings: (FeedEntity) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(vertical = 8.dp)) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
        ) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
            DrawerRow(
                icon = Icons.Outlined.WatchLater,
                label = stringResource(R.string.all_articles),
                badge = unreadCount,
                selected = scope == Scope.All,
                onClick = { onSelectScope(Scope.All) },
            )
            DrawerRow(
                icon = Icons.Filled.Star,
                label = stringResource(R.string.starred),
                badge = starredCount,
                selected = scope == Scope.Starred,
                onClick = { onSelectScope(Scope.Starred) },
            )
            HorizontalDivider(Modifier.padding(vertical = 6.dp))

            feeds.filter { it.folderId == null }.forEach { feed ->
                DrawerFeedRow(feed, scope, onSelectScope, onFeedSettings = onFeedSettings)
            }

            folders.forEach { folder ->
                val folderFeeds = feeds.filter { it.folderId == folder.id }
                DrawerRow(
                    icon = Icons.Outlined.Folder,
                    label = folder.name,
                    badge = folderFeeds.sumOf { it.unreadCount }.toInt(),
                    selected = scope == Scope.Folder(folder.id),
                    onClick = { onSelectScope(Scope.Folder(folder.id)) },
                )
                folderFeeds.forEach { feed ->
                    DrawerFeedRow(
                        feed,
                        scope,
                        onSelectScope,
                        onFeedSettings = onFeedSettings,
                        indented = true,
                    )
                }
            }
        }

        HorizontalDivider()
        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            DrawerRow(
                icon = Icons.Outlined.Settings,
                label = stringResource(R.string.settings),
                badge = 0,
                selected = false,
                onClick = onOpenSettings,
            )
        }
    }
}

@Composable
private fun DrawerRow(
    icon: ImageVector,
    label: String,
    badge: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (badge > 0) {
            Text(
                text = badge.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DrawerFeedRow(
    feed: FeedEntity,
    scope: Scope,
    onSelectScope: (Scope) -> Unit,
    onFeedSettings: (FeedEntity) -> Unit,
    indented: Boolean = false,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (scope == Scope.Feed(feed.id)) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .combinedClickable(
                onClick = { onSelectScope(Scope.Feed(feed.id)) },
                onLongClick = { onFeedSettings(feed) },
            )
            .padding(start = if (indented) 24.dp else 12.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (feed.faviconLink.isNotBlank()) {
            AsyncImage(
                model = feed.faviconLink,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        } else {
            Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = feed.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (feed.unreadCount > 0) {
            Text(
                text = feed.unreadCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Visible entry to the per-feed settings, so it does not depend on a long press.
        IconButton(
            onClick = { onFeedSettings(feed) },
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.feed_settings),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ArticleList(
    items: List<ItemSummary>,
    feeds: List<FeedEntity>,
    swipeRight: SwipeAction,
    swipeLeft: SwipeAction,
    feedListStyle: FeedListStyle,
    onToggleRead: (Long, Boolean) -> Unit,
    onToggleStarred: (Long, Boolean) -> Unit,
    onItemClick: (ItemSummary) -> Unit,
) {
    if (items.isEmpty()) {
        // Scrollable so pull-to-refresh also works with an empty list.
        Box(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.no_articles), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val feedById = feeds.associateBy { it.id }
    LazyColumn(Modifier.fillMaxSize()) {
        items(items, key = { it.id }) { item ->
            ArticleRow(
                item = item,
                feed = feedById[item.feedId],
                swipeRight = swipeRight,
                swipeLeft = swipeLeft,
                showThumbnail = feedListStyle == FeedListStyle.THUMBNAILS,
                onClick = { onItemClick(item) },
                onToggleRead = onToggleRead,
                onToggleStarred = onToggleStarred,
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun ArticleRow(
    item: ItemSummary,
    feed: FeedEntity?,
    swipeRight: SwipeAction,
    swipeLeft: SwipeAction,
    showThumbnail: Boolean,
    onClick: () -> Unit,
    onToggleRead: (Long, Boolean) -> Unit,
    onToggleStarred: (Long, Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var offsetX by remember { mutableFloatStateOf(0f) }
    val commitPx = with(density) { 140.dp.toPx() }
    val revealPx = with(density) { 48.dp.toPx() }
    val thumbnail = if (showThumbnail) item.imageUrl else null

    // Only a full swipe runs the action; a shorter one just springs back.
    val action = when {
        offsetX > revealPx -> swipeRight
        offsetX < -revealPx -> swipeLeft
        else -> SwipeAction.NOTHING
    }

    fun runAction(action: SwipeAction) {
        when (action) {
            SwipeAction.STAR -> if (!item.starred) onToggleStarred(item.id, item.starred)
            SwipeAction.READ -> if (item.unread) onToggleRead(item.id, item.unread)
            SwipeAction.NOTHING -> Unit
        }
    }

    Box(Modifier.fillMaxWidth().height(96.dp)) {
        // Hint behind the row: green check for read, amber star for favourite.
        if (action != SwipeAction.NOTHING) {
            val progress = (kotlin.math.abs(offsetX) / commitPx).coerceIn(0f, 1f)
            val (background, icon) = when (action) {
                SwipeAction.READ -> Color(0xFF2E7D32) to Icons.Filled.CheckCircle
                SwipeAction.STAR -> Color(0xFFFFC107) to Icons.Filled.Star
                SwipeAction.NOTHING -> Color.Transparent to Icons.Filled.CheckCircle
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(background.copy(alpha = 0.25f + 0.75f * progress)),
                contentAlignment = if (offsetX > 0) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f + 0.4f * progress),
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .size((24 + 8 * progress).dp),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .offset { IntOffset(offsetX.toInt(), 0) }
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onClick)
                .pointerInput(item.id, swipeRight, swipeLeft) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val travel = offsetX
                            val target = when {
                                travel >= commitPx -> swipeRight
                                travel <= -commitPx -> swipeLeft
                                else -> SwipeAction.NOTHING
                            }
                            runAction(target)
                            scope.launch {
                                androidx.compose.animation.core.animate(
                                    initialValue = travel,
                                    targetValue = 0f,
                                    animationSpec = tween(180),
                                ) { value, _ -> offsetX = value }
                            }
                        },
                        onHorizontalDrag = { change, amount ->
                            offsetX = (offsetX + amount).coerceIn(-commitPx * 1.3f, commitPx * 1.3f)
                            change.consume()
                        },
                    )
                }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (item.unread) FontWeight.SemiBold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (item.unread) 1f else READ_ALPHA,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (feed != null) {
                        if (feed.faviconLink.isNotBlank()) {
                            AsyncImage(
                                model = feed.faviconLink,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(14.dp)
                                    .alpha(if (item.unread) 1f else READ_ALPHA),
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = feed.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (item.unread) 1f else READ_ALPHA,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (item.unread) 1f else READ_ALPHA,
                            ),
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = relativeTime(item.pubDate).format(context),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (item.unread) 1f else READ_ALPHA,
                        ),
                    )
                }
            }
            if (thumbnail != null) {
                Spacer(Modifier.width(12.dp))
                AsyncImage(
                    model = thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .alpha(if (item.unread) 1f else READ_ALPHA)
                        .clip(RoundedCornerShape(8.dp)),
                )
            }
        }
    }
}

// Videos play inline with ExoPlayer. The poster is shown until the user taps it.
@Composable
private fun VideoCard(url: String, poster: String?) {
    val context = LocalContext.current
    var playing by remember(url) { mutableStateOf(false) }
    var failed by remember(url) { mutableStateOf(false) }
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true,
            )
            setMediaItem(MediaItem.fromUri(url))
            prepare()
        }
    }
    DisposableEffect(url) {
        onDispose {
            player.release()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        if (playing && !failed) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                    }
                },
                update = { view -> view.player = player },
                modifier = Modifier.fillMaxSize(),
            )
            LaunchedEffect(player) {
                player.addListener(
                    object : Player.Listener {
                        override fun onPlayerError(error: PlaybackException) {
                            failed = true
                        }
                    }
                )
            }
        } else {
            if (poster != null) {
                AsyncImage(
                    model = poster,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            IconButton(
                onClick = {
                    if (failed) {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    } else {
                        playing = true
                        player.play()
                    }
                },
                modifier = Modifier.size(72.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp),
                )
            }
            if (failed) {
                Text(
                    text = stringResource(R.string.video_playback_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp),
                )
            }
        }
    }
}

private const val READ_ALPHA = 0.55f

@Composable
private fun Reader(
    items: List<ItemSummary>,
    initialIndex: Int,
    feeds: List<FeedEntity>,
    serverUrl: String,
    isDark: Boolean,
    fullContent: Map<Long, String>,
    itemDetails: Map<Long, ItemEntity>,
    navigateWithVolume: Boolean,
    hideRelated: Boolean,
    fullTextFeedIds: Set<Long>,
    onEditFeed: (Long) -> Unit,
    onAutoRead: (Long) -> Unit,
    onLoadDetail: (Long) -> Unit,
    onFetchFull: (Long) -> Unit,
    onBack: () -> Unit,
    onToggleRead: (Long, Boolean) -> Unit,
    onToggleStarred: (Long, Boolean) -> Unit,
) {
    BackHandler(onBack = onBack)
    if (items.isEmpty()) {
        onBack()
        return
    }
    val pagerState = rememberPagerState(initialPage = initialIndex.coerceIn(0, items.lastIndex)) { items.size }
    val current = items.getOrNull(pagerState.currentPage) ?: items.first()
    // Opening an article marks it read, unless the user chose to keep it unread.
    LaunchedEffect(current.id) { onAutoRead(current.id) }
    val currentDetail = itemDetails[current.id]
    val feedTitle = feeds.firstOrNull { it.id == current.feedId }?.title

    val keyScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    if (navigateWithVolume) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (navigateWithVolume) {
                    Modifier
                        .focusRequester(focusRequester)
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            when (event.key) {
                                // Down goes to the next article, up to the previous one,
                                // matching the Nextcloud News client.
                                Key.VolumeDown -> {
                                    if (event.type == KeyEventType.KeyDown) {
                                        keyScope.launch {
                                            pagerState.animateScrollToPage(
                                                (pagerState.currentPage + 1).coerceAtMost(items.lastIndex)
                                            )
                                        }
                                    }
                                    true
                                }
                                Key.VolumeUp -> {
                                    if (event.type == KeyEventType.KeyDown) {
                                        keyScope.launch {
                                            pagerState.animateScrollToPage(
                                                (pagerState.currentPage - 1).coerceAtLeast(0)
                                            )
                                        }
                                    }
                                    true
                                }
                                else -> false
                            }
                        }
                } else {
                    Modifier
                }
            ),
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ReaderTopBar(
                item = current,
                unread = currentDetail?.unread ?: current.unread,
                feedTitle = feedTitle,
                onEditFeed = { onEditFeed(current.feedId) },
                onBack = onBack,
                onToggleRead = { onToggleRead(current.id, currentDetail?.unread ?: current.unread) },
                onFetchFull = { onFetchFull(current.id) },
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                key = { page -> items[page].id },
            ) { page ->
                val pageItem = items[page]
                val detail = itemDetails[pageItem.id]
                val favicon = feeds.firstOrNull { it.id == pageItem.feedId }?.faviconLink
                LaunchedEffect(pageItem.id) {
                    onLoadDetail(pageItem.id)
                    // Feeds marked in their settings get the full text on open.
                    if (pageItem.feedId in fullTextFeedIds && itemDetails[pageItem.id]?.fullBody == null) {
                        onFetchFull(pageItem.id)
                    }
                }
                ReaderPage(
                    item = pageItem,
                    faviconUrl = favicon,
                    isDark = isDark,
                    body = fullContent[pageItem.id] ?: detail?.fullBody ?: detail?.body,
                    hideRelated = hideRelated,
                )
            }
        }
        FloatingActions(
            item = current,
            unread = currentDetail?.unread ?: current.unread,
            starred = currentDetail?.starred ?: current.starred,
            onToggleRead = { onToggleRead(current.id, currentDetail?.unread ?: current.unread) },
            onToggleStarred = { onToggleStarred(current.id, currentDetail?.starred ?: current.starred) },
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(16.dp),
        )
    }
}

@Composable
private fun ReaderTopBar(
    item: ItemSummary,
    unread: Boolean,
    feedTitle: String?,
    onEditFeed: () -> Unit,
    onBack: () -> Unit,
    onToggleRead: () -> Unit,
    onFetchFull: () -> Unit,
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = feedTitle ?: stringResource(R.string.feed),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, item.title)
                    putExtra(Intent.EXTRA_TEXT, item.url)
                }
                context.startActivity(Intent.createChooser(send, null))
            },
        ) {
            Icon(
                Icons.Outlined.Share,
                contentDescription = stringResource(R.string.share),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        var menuY by remember { mutableIntStateOf(0) }
        Box(
            Modifier.onGloballyPositioned { coords ->
                menuY = (coords.positionInRoot().y + coords.size.height).toInt()
            },
        ) {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            AppMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                topOffset = with(LocalDensity.current) { menuY.toDp() },
                endPadding = 4.dp,
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.fetch_full)) },
                    onClick = {
                        menuOpen = false
                        onFetchFull()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.feed_settings)) },
                    onClick = {
                        menuOpen = false
                        onEditFeed()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.open_original)) },
                    onClick = {
                        menuOpen = false
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (unread) R.string.mark_read else R.string.mark_unread
                            )
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onToggleRead()
                    },
                )
            }
        }
    }
}

@Composable
private fun ReaderPage(
    item: ItemSummary,
    faviconUrl: String?,
    isDark: Boolean,
    body: String?,
    hideRelated: Boolean,
) {
    val context = LocalContext.current
    val linkColor = MaterialTheme.colorScheme.primary
    // The body becomes a list of text and image blocks, in document order, so it
    // is laid out lazily and the images stay where the article puts them.
    // Parsing HTML takes hundreds of milliseconds, so keep it off the main thread.
    val blocks by produceState<List<ReaderBlock>?>(
        initialValue = null,
        body,
        linkColor,
        hideRelated,
    ) {
        value = body?.let { html ->
            withContext(Dispatchers.Default) { htmlToBlocks(html, linkColor, hideRelated) }
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
    ) {
        item {
            Column {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp, lineHeight = 26.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!faviconUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = faviconUrl,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = item.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = relativeTime(item.pubDate).format(context),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                item.mediaThumbnail?.let { heroUrl ->
                    Spacer(Modifier.height(12.dp))
                    AsyncImage(
                        model = heroUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
        val ready = blocks
        when {
            ready == null && body != null -> item {
                CircularProgressIndicator(Modifier.padding(top = 8.dp))
            }
            ready != null -> items(ready.size) { index ->
                when (val block = ready[index]) {
                    is ReaderBlock.Text -> Text(
                        text = block.content,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    is ReaderBlock.Video -> VideoCard(block.url, block.poster)
                    is ReaderBlock.Image -> AsyncImage(
                        model = block.url,
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                }
            }
        }
    }
}

private fun Char.isVisibleChar(): Boolean = !isWhitespace() && this != '\u00A0'

// Source of a <video> tag, either in its attributes or in a nested <source>.
private fun videoSource(attrs: String, inner: String): String? {
    val src = Regex("""src\s*=\s*[\"']([^\"']+)[\"']""", RegexOption.IGNORE_CASE)
    val raw = src.find(attrs)?.groupValues?.getOrNull(1)
        ?: src.find(inner)?.groupValues?.getOrNull(1)
        ?: return null
    return unpackImageUrl(raw)
}

private sealed interface ReaderBlock {
    // card blocks are boilerplate: a label plus a link, whatever the wording.
    data class Text(val content: AnnotatedString, val card: Boolean = false) : ReaderBlock
    data class Image(val url: String) : ReaderBlock
    // Video embeds cannot play inline yet, so they show a poster and open outside.
    data class Video(val url: String, val poster: String?) : ReaderBlock
}

// Turns the article HTML into text and image blocks in document order, so the
// images are rendered where the article places them.
private fun htmlToBlocks(html: String, linkColor: Color, hideRelated: Boolean): List<ReaderBlock> {
    // Cards are removed from the HTML first: they come glued to the next
    // paragraph, so block-level filtering cannot see them whole.
    val source = if (hideRelated) stripCardLinkRuns(html) else html
    val imgTag = Regex("""<img[^>]*>""", RegexOption.IGNORE_CASE)
    val srcAttr = Regex("""src\s*=\s*[\"']([^\"']+)[\"']""", RegexOption.IGNORE_CASE)
    val posterAttr = Regex("""poster\s*=\s*[\"']([^\"']+)[\"']""", RegexOption.IGNORE_CASE)
    val result = mutableListOf<ReaderBlock>()

    // Videos are pulled out first: feed readability sometimes keeps the <video> tag.
    val videos = mutableListOf<Triple<IntRange, String, String?>>()
    for (match in Regex("""(?is)<video\b([^>]*)>(.*?)</video>""").findAll(source)) {
        val url = videoSource(match.groupValues[1], match.groupValues[2]) ?: continue
        val poster = posterAttr.find(match.groupValues[1])?.groupValues?.getOrNull(1)
            ?.let { unpackImageUrl(it) }
        videos.add(Triple(match.range, url, poster))
    }
    for (match in Regex("""(?is)<video\b([^>]*)/>""").findAll(source)) {
        if (videos.any { match.range.first in it.first }) continue
        val url = videoSource(match.groupValues[1], "") ?: continue
        val poster = posterAttr.find(match.groupValues[1])?.groupValues?.getOrNull(1)
            ?.let { unpackImageUrl(it) }
        videos.add(Triple(match.range, url, poster))
    }
    val videoAt = videos.sortedBy { it.first.first }

    fun addText(chunk: String) {
        for (part in splitHtmlBlocks(chunk)) {
            val text = htmlToAnnotated(part, linkColor)
            // Html.fromHtml pads blocks with newlines and empty paragraphs come as
            // &nbsp;, so trim the visible range to avoid blank gaps.
            val start = text.indexOfFirst { it.isVisibleChar() }
            if (start < 0) continue
            val end = text.indexOfLast { it.isVisibleChar() }
            result.add(
                ReaderBlock.Text(
                    content = text.subSequence(start, end + 1),
                    card = isCardBlock(part),
                )
            )
        }
    }

    data class Media(val start: Int, val end: Int, val block: ReaderBlock)

    val media = mutableListOf<Media>()
    for (video in videoAt) {
        media.add(
            Media(video.first.first, video.first.last + 1, ReaderBlock.Video(video.second, video.third))
        )
    }
    for (match in imgTag.findAll(source)) {
        if (videoAt.any { match.range.first in it.first }) continue
        val src = srcAttr.find(match.value)?.groupValues?.getOrNull(1) ?: continue
        media.add(Media(match.range.first, match.range.last + 1, ReaderBlock.Image(unpackImageUrl(src))))
    }
    media.sortBy { it.start }

    var cursor = 0
    for (element in media) {
        if (element.start < cursor) continue
        if (element.start > cursor) addText(source.substring(cursor, element.start))
        result.add(element.block)
        cursor = element.end
    }
    if (cursor < source.length) addText(source.substring(cursor))
    if (!hideRelated) return result
    // Sites append a "you may also like" block at the end; drop the heading and
    // everything after it.
    val cut = result.indexOfFirst { block ->
        block is ReaderBlock.Text && isRelatedHeading(block.content.text)
    }
    val kept = if (cut >= 0) result.subList(0, cut) else result
    return kept.filterNot { block ->
        block is ReaderBlock.Text &&
            // Single-line cross references such as "En Xataka | <other article>",
            // and cards (label plus link) wherever the site places them.
            (isRelatedLink(block.content.text) || block.card)
    }
}

// Splits an article body on block-level closing tags so each paragraph can be
// parsed and laid out on its own. Inline tags and <br> stay inside a block.
private fun splitHtmlBlocks(html: String): List<String> {
    val marked = Regex("""(?i)</p>|</div>|</h[1-6]>|</li>|</blockquote>|</td>|</tr>""")
        .replace(html) { match -> match.value + "\u0000" }
    return marked.split("\u0000").map { it.trim() }.filter { it.isNotEmpty() }
}

// Converts a HTML fragment into a Compose AnnotatedString keeping bold, italic,
// underline and links; images are handled separately as ReaderBlock.Image.
private fun htmlToAnnotated(html: String, linkColor: Color): AnnotatedString {
    val spanned = android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY)
    return buildAnnotatedString {
        append(spanned.toString())
        for (span in spanned.getSpans(0, spanned.length, Any::class.java)) {
            val start = spanned.getSpanStart(span).coerceAtLeast(0)
            val end = spanned.getSpanEnd(span).coerceAtMost(spanned.length)
            if (end <= start) continue
            when (span) {
                is android.text.style.StyleSpan -> {
                    val bold = span.style and android.graphics.Typeface.BOLD != 0
                    val italic = span.style and android.graphics.Typeface.ITALIC != 0
                    addStyle(
                        SpanStyle(
                            fontWeight = if (bold) FontWeight.Bold else null,
                            fontStyle = if (italic) FontStyle.Italic else null,
                        ),
                        start,
                        end,
                    )
                }
                is android.text.style.URLSpan -> {
                    addLink(LinkAnnotation.Url(span.url), start, end)
                    addStyle(SpanStyle(color = linkColor), start, end)
                }
            }
        }
    }
}

@Composable
private fun FloatingActions(
    item: ItemSummary,
    unread: Boolean,
    starred: Boolean,
    onToggleRead: () -> Unit,
    onToggleStarred: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 6.dp,
    ) {
        Row(Modifier.padding(horizontal = 4.dp)) {
            IconButton(onClick = onToggleStarred) {
                Icon(
                    imageVector = if (starred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = stringResource(R.string.star),
                    tint = if (starred) StarGold else LocalContentColor.current,
                )
            }
            IconButton(onClick = onToggleRead) {
                Icon(
                    imageVector = if (unread) Icons.Outlined.RadioButtonUnchecked else Icons.Filled.CheckCircle,
                    contentDescription = stringResource(
                        if (unread) R.string.mark_read else R.string.mark_unread
                    ),
                    tint = if (unread) LocalContentColor.current else MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
                },
            ) {
                Icon(
                    Icons.Outlined.OpenInNew,
                    contentDescription = stringResource(R.string.open_in_browser),
                )
            }
        }
    }
}

@Composable
private fun scopeLabel(scope: Scope, folders: List<FolderEntity>): String = when (scope) {
    Scope.All -> stringResource(R.string.all_articles)
    Scope.Starred -> stringResource(R.string.starred)
    is Scope.Feed -> stringResource(R.string.feed)
    is Scope.Folder -> folders.firstOrNull { it.id == scope.id }?.name ?: stringResource(R.string.feed)
}

// The drawer is half the screen on a phone, narrower on wide screens, and never
// wider than MAX_DRAWER_DP, like the official Nextcloud clients.
private const val MIN_DRAWER_DP = 240f
private const val MAX_DRAWER_DP = 320f

private fun drawerWidth(screenWidthDp: Int): Dp {
    val width = if (screenWidthDp > 0) screenWidthDp.toFloat() else 360f
    return (width * 0.5f).coerceIn(MIN_DRAWER_DP, MAX_DRAWER_DP).dp
}
