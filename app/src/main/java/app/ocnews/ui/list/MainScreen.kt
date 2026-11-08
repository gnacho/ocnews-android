package app.ocnews.ui.list

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.ocnews.BuildConfig
import app.ocnews.R
import app.ocnews.data.local.FeedEntity
import app.ocnews.data.local.FolderEntity
import app.ocnews.data.local.ItemEntity
import app.ocnews.data.settings.AppSettings
import app.ocnews.data.settings.FeedListStyle
import app.ocnews.data.settings.SwipeAction
import app.ocnews.ui.addfeed.AddFeedDialog
import app.ocnews.ui.addfeed.AddFeedViewModel
import app.ocnews.util.format
import app.ocnews.util.relativeTime
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

private val StarGold = Color(0xFFFFB300)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    folders: List<FolderEntity>,
    feeds: List<FeedEntity>,
    items: List<ItemEntity>,
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
    onFetchFull: (Long) -> Unit,
    addFeedViewModel: AddFeedViewModel,
    onSelectScope: (Scope) -> Unit,
    onToggleOnlyUnread: () -> Unit,
    onToggleRead: (ItemEntity) -> Unit,
    onToggleStarred: (ItemEntity) -> Unit,
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

    if (showAddFeed) {
        AddFeedDialog(
            viewModel = addFeedViewModel,
            onDismiss = { showAddFeed = false },
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
                                Box {
                                    IconButton(onClick = { profileMenuOpen = true }) {
                                        Icon(Icons.Filled.MoreVert, contentDescription = null)
                                    }
                                    DropdownMenu(
                                        expanded = profileMenuOpen,
                                        onDismissRequest = { profileMenuOpen = false },
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
        } else {
            Reader(
                items = items,
                initialIndex = selectedIndex ?: 0,
                feeds = feeds,
                serverUrl = serverUrl,
                isDark = isDark,
                fullContent = fullContent,
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
    items: List<ItemEntity>,
    feeds: List<FeedEntity>,
    swipeRight: SwipeAction,
    swipeLeft: SwipeAction,
    feedListStyle: FeedListStyle,
    onToggleRead: (ItemEntity) -> Unit,
    onToggleStarred: (ItemEntity) -> Unit,
    onItemClick: (ItemEntity) -> Unit,
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
    item: ItemEntity,
    feed: FeedEntity?,
    swipeRight: SwipeAction,
    swipeLeft: SwipeAction,
    showThumbnail: Boolean,
    onClick: () -> Unit,
    onToggleRead: (ItemEntity) -> Unit,
    onToggleStarred: (ItemEntity) -> Unit,
) {
    val context = LocalContext.current
    var offsetX by remember { mutableStateOf(0f) }
    val thumbnail = if (showThumbnail) {
        item.mediaThumbnail ?: firstImageUrl(item.body)?.let { unpackImageUrl(it) }
    } else {
        null
    }
    fun runAction(action: SwipeAction) {
        when (action) {
            SwipeAction.STAR -> if (!item.starred) onToggleStarred(item)
            SwipeAction.READ -> if (item.unread) onToggleRead(item)
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

// Extracts the first <img src> from an article body to use as a hero image.
private fun firstImageUrl(html: String): String? {
    val regex = Regex("""<img[^>]+src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    return regex.find(html)?.groupValues?.getOrNull(1)
}

// ocnews rewrites article images to its own proxy (/img?u=<encoded>), which currently
// returns 403. Unpack the original image URL from the `u` parameter (also decodes &amp;).
private fun unpackImageUrl(src: String): String {
    val decoded = src.replace("&amp;", "&")
    if (!decoded.contains("/img?")) return decoded
    val u = Regex("""[?&]u=([^&]+)""").find(decoded)?.groupValues?.getOrNull(1) ?: return decoded
    return try {
        java.net.URLDecoder.decode(u, "UTF-8")
    } catch (e: Exception) {
        decoded
    }
}

// Rewrites the article body's proxied image srcs to the original image URLs.
private fun rewriteBodyImages(html: String): String =
    Regex("""(<img[^>]*?src=["'])([^"']+)(["'])""", RegexOption.IGNORE_CASE).replace(html) { m ->
        m.groupValues[1] + unpackImageUrl(m.groupValues[2]) + m.groupValues[3]
    }

@Composable
private fun Reader(
    items: List<ItemEntity>,
    initialIndex: Int,
    feeds: List<FeedEntity>,
    serverUrl: String,
    isDark: Boolean,
    fullContent: Map<Long, String>,
    onFetchFull: (Long) -> Unit,
    onBack: () -> Unit,
    onToggleRead: (ItemEntity) -> Unit,
    onToggleStarred: (ItemEntity) -> Unit,
) {
    BackHandler(onBack = onBack)
    if (items.isEmpty()) {
        onBack()
        return
    }
    val pagerState = rememberPagerState(initialPage = initialIndex.coerceIn(0, items.lastIndex)) { items.size }
    val current = items.getOrNull(pagerState.currentPage) ?: items.first()
    val feedTitle = feeds.firstOrNull { it.id == current.feedId }?.title

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ReaderTopBar(
                item = current,
                feedTitle = feedTitle,
                onBack = onBack,
                onToggleRead = onToggleRead,
                onFetchFull = { onFetchFull(current.id) },
            )
            val scope = rememberCoroutineScope()
            val dragState = rememberDraggableState { delta ->
                pagerState.dispatchRawDelta(-delta)
            }
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier.weight(1f).draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        val currentPage = pagerState.currentPage
                        val target = when {
                            velocity < -500f -> currentPage + 1
                            velocity > 500f -> currentPage - 1
                            else -> kotlin.math.round(
                                currentPage + pagerState.currentPageOffsetFraction
                            ).toInt()
                        }
                        scope.launch {
                            pagerState.animateScrollToPage(target.coerceIn(0, items.lastIndex))
                        }
                    },
                ),
                key = { page -> items[page].id },
            ) { page ->
                val pageItem = items[page]
                ReaderPage(
                    item = pageItem,
                    serverUrl = serverUrl,
                    isDark = isDark,
                    body = fullContent[pageItem.id] ?: pageItem.body,
                )
            }
        }
        FloatingActions(
            item = current,
            onToggleRead = onToggleRead,
            onToggleStarred = onToggleStarred,
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(16.dp),
        )
    }
}

@Composable
private fun ReaderTopBar(
    item: ItemEntity,
    feedTitle: String?,
    onBack: () -> Unit,
    onToggleRead: (ItemEntity) -> Unit,
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
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
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
                                if (item.unread) R.string.mark_read else R.string.mark_unread
                            )
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onToggleRead(item)
                    },
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ReaderPage(item: ItemEntity, serverUrl: String, isDark: Boolean, body: String) {
    val context = LocalContext.current
    val html = remember(item.id, body, isDark) { buildReaderHtml(context, item, body, isDark) }
    val bgColor = if (isDark) android.graphics.Color.parseColor("#121212") else android.graphics.Color.WHITE
    // Recreate the WebView when the body changes (fetch), and destroy it on release
    // to avoid leaking native memory while swiping between articles.
    key(html) {
        val holder = remember { arrayOfNulls<WebView>(1) }
        DisposableEffect(Unit) {
            onDispose {
                holder[0]?.let { it.stopLoading(); it.destroy() }
                holder[0] = null
            }
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    setBackgroundColor(bgColor)
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                    holder[0] = this
                    loadDataWithBaseURL(serverUrl, html, "text/html", "UTF-8", null)
                }
            },
        )
    }
}

// Single-scroll reader HTML: optional hero image, title, meta and the article body.
private fun buildReaderHtml(context: Context, item: ItemEntity, body: String, dark: Boolean): String {
    val mediaThumb = item.mediaThumbnail
    val bodyFirstRaw = firstImageUrl(body)
    val heroUrl = mediaThumb ?: bodyFirstRaw?.let { unpackImageUrl(it) }
    var content = rewriteBodyImages(body)
    if (mediaThumb == null && bodyFirstRaw != null) {
        content = stripFirstImage(content)
    }
    val time = relativeTime(item.pubDate).format(context)
    val meta = buildString {
        if (item.author.isNotBlank()) append(escapeHtml(item.author)).append(" · ")
        append(time)
    }
    val bg = if (dark) "#121212" else "#FFFFFF"
    val fg = if (dark) "#E6E1E5" else "#222222"
    val metaColor = if (dark) "#9E9E9E" else "#888888"
    val linkColor = if (dark) "#D0BCFF" else "#1A73E8"
    return buildString {
        append("<!DOCTYPE html><html><head><meta charset=\"utf-8\">")
        append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
        append("<style>html,body{background:").append(bg).append(";}")
        append("body{font-family:-apple-system,Roboto,sans-serif;margin:16px;line-height:1.6;color:").append(fg).append(";}")
        append("a{color:").append(linkColor).append(";}")
        append("img,video{max-width:100%;height:auto;border-radius:8px;}")
        append("h1{font-size:1.4em;line-height:1.3;margin:12px 0 4px;color:").append(fg).append(";}")
        append(".meta{color:").append(metaColor).append(";font-size:0.85em;margin:0 0 16px;}")
        append("</style></head><body>")
        append("<h1>").append(escapeHtml(item.title)).append("</h1>")
        append("<div class=\"meta\">").append(meta).append("</div>")
        if (heroUrl != null) append("<img src=\"").append(heroUrl).append("\">")
        append(content)
        append("</body></html>")
    }
}

private fun stripFirstImage(html: String): String =
    Regex("""<img[^>]*>""", RegexOption.IGNORE_CASE).replaceFirst(html, "")

private fun escapeHtml(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

@Composable
private fun FloatingActions(
    item: ItemEntity,
    onToggleRead: (ItemEntity) -> Unit,
    onToggleStarred: (ItemEntity) -> Unit,
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
            IconButton(onClick = { onToggleStarred(item) }) {
                Icon(
                    imageVector = if (item.starred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = stringResource(R.string.star),
                    tint = if (item.starred) StarGold else LocalContentColor.current,
                )
            }
            IconButton(onClick = { onToggleRead(item) }) {
                Icon(
                    imageVector = if (item.unread) Icons.Outlined.RadioButtonUnchecked else Icons.Filled.CheckCircle,
                    contentDescription = stringResource(
                        if (item.unread) R.string.mark_read else R.string.mark_unread
                    ),
                    tint = if (item.unread) LocalContentColor.current else MaterialTheme.colorScheme.primary,
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
