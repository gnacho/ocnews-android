package app.ocnews.ui.list

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
    username: String,
    serverUrl: String,
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
    onImportOpml: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var showAddFeed by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var searchActive by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    if (showAddFeed) {
        AddFeedDialog(
            viewModel = addFeedViewModel,
            onDismiss = { showAddFeed = false },
        )
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
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
                    onLogout = onLogout,
                    onImportOpml = onImportOpml,
                    onOpenSettings = onOpenSettings,
                    onOpenAbout = { showAbout = true },
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
                                IconButton(onClick = { showAddFeed = true }) {
                                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_feed))
                                }
                                TextButton(onClick = onToggleOnlyUnread) {
                                    Text(
                                        if (onlyUnread) stringResource(R.string.unread)
                                        else stringResource(R.string.all)
                                    )
                                }
                                IconButton(onClick = onSync, enabled = !syncing) {
                                    Text(if (syncing) "…" else "↻")
                                }
                                UserAvatar(
                                    username = username,
                                    onClick = { coroutineScope.launch { drawerState.open() } },
                                )
                            }
                        },
                    )
                },
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    ArticleList(
                        items = items,
                        feeds = feeds,
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
private fun UserAvatar(username: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about)) },
        text = { Text(stringResource(R.string.about_text, BuildConfig.VERSION_NAME)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
        },
    )
}

@Composable
private fun DrawerContent(
    folders: List<FolderEntity>,
    feeds: List<FeedEntity>,
    scope: Scope,
    unreadCount: Int,
    starredCount: Int,
    onSelectScope: (Scope) -> Unit,
    onLogout: () -> Unit,
    onImportOpml: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(vertical = 16.dp)) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
        ) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.all_articles)) },
                icon = { Icon(Icons.Outlined.WatchLater, contentDescription = null) },
                badge = { DrawerBadge(unreadCount) },
                selected = scope == Scope.All,
                onClick = { onSelectScope(Scope.All) },
            )
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.starred)) },
                icon = { Icon(Icons.Filled.Star, contentDescription = null) },
                badge = { DrawerBadge(starredCount) },
                selected = scope == Scope.Starred,
                onClick = { onSelectScope(Scope.Starred) },
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            feeds.filter { it.folderId == null }.forEach { feed ->
                DrawerFeedItem(feed, scope, onSelectScope)
            }

            folders.forEach { folder ->
                val folderFeeds = feeds.filter { it.folderId == folder.id }
                NavigationDrawerItem(
                    label = { Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    icon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                    badge = { DrawerBadge(folderFeeds.sumOf { it.unreadCount }.toInt()) },
                    selected = scope == Scope.Folder(folder.id),
                    onClick = { onSelectScope(Scope.Folder(folder.id)) },
                )
                folderFeeds.forEach { feed ->
                    DrawerFeedItem(feed, scope, onSelectScope, indented = true)
                }
            }
        }

        HorizontalDivider()
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.import_opml)) },
                icon = { Icon(Icons.Outlined.FileDownload, contentDescription = null) },
                selected = false,
                onClick = onImportOpml,
            )
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.settings)) },
                icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                selected = false,
                onClick = onOpenSettings,
            )
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.about)) },
                icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                selected = false,
                onClick = onOpenAbout,
            )
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.log_out)) },
                icon = { Icon(Icons.Outlined.Logout, contentDescription = null) },
                selected = false,
                onClick = onLogout,
            )
        }
    }
}

@Composable
private fun DrawerBadge(count: Int) {
    if (count > 0) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DrawerFeedItem(
    feed: FeedEntity,
    scope: Scope,
    onSelectScope: (Scope) -> Unit,
    indented: Boolean = false,
) {
    NavigationDrawerItem(
        icon = {
            if (feed.faviconLink.isNotBlank()) {
                AsyncImage(
                    model = feed.faviconLink,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            } else {
                Icon(Icons.Outlined.Folder, contentDescription = null)
            }
        },
        label = {
            Text(
                text = feed.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (indented) Modifier.padding(start = 8.dp) else Modifier,
            )
        },
        badge = { DrawerBadge(feed.unreadCount.toInt()) },
        selected = scope == Scope.Feed(feed.id),
        onClick = { onSelectScope(Scope.Feed(feed.id)) },
    )
}

@Composable
private fun ArticleList(
    items: List<ItemEntity>,
    feeds: List<FeedEntity>,
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
    onClick: () -> Unit,
    onToggleRead: (ItemEntity) -> Unit,
    onToggleStarred: (ItemEntity) -> Unit,
) {
    val context = LocalContext.current
    var offsetX by remember { mutableStateOf(0f) }
    val thumbnail = item.mediaThumbnail ?: firstImageUrl(item.body)?.let { unpackImageUrl(it) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .offset { IntOffset(offsetX.toInt(), 0) }
            .clickable(onClick = onClick)
            .pointerInput(item.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX < -120f) {
                            if (item.unread) onToggleRead(item)
                        } else if (offsetX > 120f) {
                            if (!item.starred) onToggleStarred(item)
                        }
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
                style = MaterialTheme.typography.bodyLarge,
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
                    .size(width = 112.dp, height = 80.dp)
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

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ReaderTopBar(
                item = current,
                feedTitle = feedTitle,
                onBack = onBack,
                onToggleRead = onToggleRead,
                onFetchFull = { onFetchFull(current.id) },
            )
            val scope = rememberCoroutineScope()
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier.weight(1f).pointerInput(items.size) {
                    var accumulated = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { accumulated = 0f },
                        onDragEnd = {
                            val target = when {
                                accumulated < -150f -> pagerState.currentPage + 1
                                accumulated > 150f -> pagerState.currentPage - 1
                                else -> null
                            }
                            if (target != null) {
                                scope.launch {
                                    pagerState.animateScrollToPage(target.coerceIn(0, items.lastIndex))
                                }
                            }
                            accumulated = 0f
                        },
                        onHorizontalDrag = { change, amount ->
                            accumulated += amount
                            change.consume()
                        },
                    )
                },
                key = { page -> items[page].id },
            ) { page ->
                val pageItem = items[page]
                ReaderPage(
                    item = pageItem,
                    serverUrl = serverUrl,
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
            Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
        }
        Text(
            text = feedTitle ?: stringResource(R.string.feed),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
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
            Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.share))
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = null)
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
private fun ReaderPage(item: ItemEntity, serverUrl: String, body: String) {
    val context = LocalContext.current
    val html = remember(item.id, body) { buildReaderHtml(context, item, body) }
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
                    holder[0] = this
                    loadDataWithBaseURL(serverUrl, html, "text/html", "UTF-8", null)
                }
            },
        )
    }
}

// Single-scroll reader HTML: optional hero image, title, meta and the article body.
private fun buildReaderHtml(context: Context, item: ItemEntity, body: String): String {
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
    return buildString {
        append("<!DOCTYPE html><html><head><meta charset=\"utf-8\">")
        append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
        append("<style>body{font-family:-apple-system,Roboto,sans-serif;margin:16px;line-height:1.6;color:#222;}")
        append("img,video{max-width:100%;height:auto;border-radius:8px;}")
        append("h1{font-size:1.4em;line-height:1.3;margin:12px 0 4px;}")
        append(".meta{color:#888;font-size:0.85em;margin:0 0 16px;}")
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
