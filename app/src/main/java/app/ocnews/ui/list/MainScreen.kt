package app.ocnews.ui.list

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import app.ocnews.util.format
import app.ocnews.util.relativeTime
import app.ocnews.util.unpackImageUrl
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
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
            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.8f)) {
                DrawerContent(
                    folders = folders,
                    feeds = feeds,
                    scope = scope,
                    unreadCount = unreadCount,
                    starredCount = starredCount,
                    onSelectScope = { s ->
                        onSelectScope(s)
                        coroutineScope.launch { drawerState.close() }
                    },
                    onOpenSettings = onOpenSettings,
                )
            }
        },
    ) {
        val current = selectedIndex?.let { items.getOrNull(it) }
        if (current == null) {
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
                    ArticleList(
                        items = items,
                        feeds = feeds,
                        swipeRight = settings.swipeRight,
                        swipeLeft = settings.swipeLeft,
                        feedListStyle = settings.feedListStyle,
                        onToggleRead = onToggleRead,
                        onToggleStarred = onToggleStarred,
                        onItemClick = { item ->
                            selectedIndex = items.indexOfFirst { it.id == item.id }.takeIf { it >= 0 }
                        },
                    )
                    }
                }
            }
        } else {
            Reader(
                items = items,
                initialIndex = selectedIndex ?: 0,
                feeds = feeds,
                serverUrl = serverUrl,
                isDark = isDark,
                fullContent = fullContent,
                itemDetails = itemDetails,
                navigateWithVolume = navigateWithVolume,
                hideRelated = hideRelated,
                onLoadDetail = onLoadDetail,
                onFetchFull = onFetchFull,
                onBack = { selectedIndex = null },
                onToggleRead = onToggleRead,
                onToggleStarred = onToggleStarred,
            )
        }
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
                DrawerFeedRow(feed, scope, onSelectScope)
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
                    DrawerFeedRow(feed, scope, onSelectScope, indented = true)
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
    indented: Boolean = false,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (scope == Scope.Feed(feed.id)) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable { onSelectScope(Scope.Feed(feed.id)) }
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
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
    var offsetX by remember { mutableStateOf(0f) }
    val thumbnail = if (showThumbnail) item.imageUrl else null
    fun runAction(action: SwipeAction) {
        when (action) {
            SwipeAction.STAR -> if (!item.starred) onToggleStarred(item.id, item.starred)
            SwipeAction.READ -> if (item.unread) onToggleRead(item.id, item.unread)
            SwipeAction.NOTHING -> Unit
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .offset { IntOffset(offsetX.toInt(), 0) }
            .clickable(onClick = onClick)
            .pointerInput(item.id, swipeRight, swipeLeft) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX < -120f) runAction(swipeLeft)
                        else if (offsetX > 120f) runAction(swipeRight)
                        offsetX = 0f
                    },
                    onHorizontalDrag = { change, amount ->
                        offsetX += amount
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
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = feed.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = relativeTime(item.pubDate).format(context),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    .clip(RoundedCornerShape(8.dp)),
            )
        }
    }
}

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
                LaunchedEffect(pageItem.id) { onLoadDetail(pageItem.id) }
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

private sealed interface ReaderBlock {
    data class Text(val content: AnnotatedString) : ReaderBlock
    data class Image(val url: String) : ReaderBlock
}

// Turns the article HTML into text and image blocks in document order, so the
// images are rendered where the article places them.
private fun htmlToBlocks(html: String, linkColor: Color, hideRelated: Boolean): List<ReaderBlock> {
    val imgTag = Regex("""<img[^>]*>""", RegexOption.IGNORE_CASE)
    val srcAttr = Regex("""src\s*=\s*[\"']([^\"']+)[\"']""", RegexOption.IGNORE_CASE)
    val result = mutableListOf<ReaderBlock>()

    fun addText(chunk: String) {
        for (part in splitHtmlBlocks(chunk)) {
            val text = htmlToAnnotated(part, linkColor)
            // Html.fromHtml pads blocks with newlines and empty paragraphs come as
            // &nbsp;, so trim the visible range to avoid blank gaps.
            val start = text.indexOfFirst { it.isVisibleChar() }
            if (start < 0) continue
            val end = text.indexOfLast { it.isVisibleChar() }
            result.add(ReaderBlock.Text(text.subSequence(start, end + 1)))
        }
    }

    var cursor = 0
    for (match in imgTag.findAll(html)) {
        if (match.range.first > cursor) addText(html.substring(cursor, match.range.first))
        val src = srcAttr.find(match.value)?.groupValues?.getOrNull(1)
        if (!src.isNullOrBlank()) result.add(ReaderBlock.Image(unpackImageUrl(src)))
        cursor = match.range.last + 1
    }
    if (cursor < html.length) addText(html.substring(cursor))
    if (!hideRelated) return result
    // Sites append a "you may also like" block at the end; drop it and what follows.
    val cut = result.indexOfFirst { block ->
        block is ReaderBlock.Text && isRelatedHeading(block.content.text)
    }
    return if (cut >= 0) result.subList(0, cut).toList() else result
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
