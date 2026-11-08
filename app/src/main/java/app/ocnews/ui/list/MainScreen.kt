package app.ocnews.ui.list

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
                                Text(scopeLabel(scope, folders))
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
    val context = LocalContext.current
    val feedById = feeds.associateBy { it.id }
    LazyColumn(Modifier.fillMaxSize()) {
        items(items, key = { it.id }) { item ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(item) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (item.unread) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onToggleStarred(item) }) {
                        Icon(
                            imageVector = if (item.starred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = stringResource(R.string.star),
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val feed = feedById[item.feedId]
                    if (feed != null) {
                        if (feed.faviconLink.isNotBlank()) {
                            AsyncImage(
                                model = feed.faviconLink,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = feed.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = relativeTime(item.pubDate).format(context),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider()
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
        Column(Modifier.fillMaxSize()) {
            ReaderTopBar(
                item = current,
                feedTitle = feedTitle,
                onBack = onBack,
                onToggleRead = onToggleRead,
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                key = { page -> items[page].id },
            ) { page ->
                ReaderPage(items[page], serverUrl)
            }
        }
        Box(Modifier.align(Alignment.BottomEnd)) {
            FloatingActions(
                item = current,
                onToggleRead = onToggleRead,
                onToggleStarred = onToggleStarred,
            )
        }
    }
}

@Composable
private fun ReaderTopBar(
    item: ItemEntity,
    feedTitle: String?,
    onBack: () -> Unit,
    onToggleRead: (ItemEntity) -> Unit,
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
private fun ReaderPage(item: ItemEntity, serverUrl: String) {
    val context = LocalContext.current
    val heroUrl = item.mediaThumbnail ?: firstImageUrl(item.body)?.let { unpackImageUrl(it) }
    Column(Modifier.fillMaxSize()) {
        if (heroUrl != null) {
            AsyncImage(
                model = heroUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(220.dp),
            )
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            if (item.author.isNotBlank()) {
                Text(
                    text = item.author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = relativeTime(item.pubDate).format(context),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AndroidView(
            modifier = Modifier.weight(1f),
            factory = { ctx ->
                WebView(ctx).apply {
                    loadDataWithBaseURL(serverUrl, rewriteBodyImages(item.body), "text/html", "UTF-8", null)
                }
            },
        )
    }
}

@Composable
private fun FloatingActions(
    item: ItemEntity,
    onToggleRead: (ItemEntity) -> Unit,
    onToggleStarred: (ItemEntity) -> Unit,
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.padding(16.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 6.dp,
    ) {
        Row(Modifier.padding(horizontal = 4.dp)) {
            IconButton(onClick = { onToggleStarred(item) }) {
                Icon(
                    imageVector = if (item.starred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = stringResource(R.string.star),
                )
            }
            IconButton(onClick = { onToggleRead(item) }) {
                Icon(
                    imageVector = if (item.unread) Icons.Outlined.CheckCircle else Icons.Filled.CheckCircle,
                    contentDescription = stringResource(
                        if (item.unread) R.string.mark_read else R.string.mark_unread
                    ),
                )
            }
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
