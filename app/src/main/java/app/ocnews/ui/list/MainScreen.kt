package app.ocnews.ui.list

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
    var selectedItem by remember { mutableStateOf<ItemEntity?>(null) }
    var showAddFeed by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    if (showAddFeed) {
        AddFeedDialog(
            viewModel = addFeedViewModel,
            onDismiss = { showAddFeed = false },
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    folders = folders,
                    feeds = feeds,
                    scope = scope,
                    onSelectScope = { s ->
                        onSelectScope(s)
                        coroutineScope.launch { drawerState.close() }
                    },
                    onLogout = onLogout,
                    onImportOpml = onImportOpml,
                    onOpenSettings = onOpenSettings,
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(scopeLabel(scope)) },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Text("☰")
                        }
                    },
                    actions = {
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
                    },
                )
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                val current = selectedItem
                if (current == null) {
                    ArticleList(
                        items = items,
                        feeds = feeds,
                        onToggleRead = onToggleRead,
                        onToggleStarred = onToggleStarred,
                        onItemClick = { selectedItem = it },
                    )
                } else {
                    Reader(
                        item = current,
                        onBack = { selectedItem = null },
                        onToggleRead = onToggleRead,
                        onToggleStarred = onToggleStarred,
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerContent(
    folders: List<FolderEntity>,
    feeds: List<FeedEntity>,
    scope: Scope,
    onSelectScope: (Scope) -> Unit,
    onLogout: () -> Unit,
    onImportOpml: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.all_articles)) },
            selected = scope == Scope.All,
            onClick = { onSelectScope(Scope.All) },
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.starred)) },
            selected = scope == Scope.Starred,
            onClick = { onSelectScope(Scope.Starred) },
        )
        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        val rootFeeds = feeds.filter { it.folderId == null }
        rootFeeds.forEach { feed ->
            DrawerFeedItem(feed, scope, onSelectScope)
        }

        folders.forEach { folder ->
            Text(
                text = folder.name,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            )
            feeds.filter { it.folderId == folder.id }.forEach { feed ->
                DrawerFeedItem(feed, scope, onSelectScope)
            }
        }

        Spacer(Modifier.weight(1f))
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.import_opml)) },
            selected = false,
            onClick = onImportOpml,
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.settings)) },
            selected = false,
            onClick = onOpenSettings,
        )
        TextButton(onClick = onLogout) { Text(stringResource(R.string.log_out)) }
    }
}

@Composable
private fun DrawerFeedItem(
    feed: FeedEntity,
    scope: Scope,
    onSelectScope: (Scope) -> Unit,
) {
    NavigationDrawerItem(
        icon = {
            if (feed.faviconLink.isNotBlank()) {
                AsyncImage(
                    model = feed.faviconLink,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        label = {
            Text(
                text = feed.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
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
                    if (item.mediaThumbnail != null) {
                        AsyncImage(
                            model = item.mediaThumbnail,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                    }
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun Reader(
    item: ItemEntity,
    onBack: () -> Unit,
    onToggleRead: (ItemEntity) -> Unit,
    onToggleStarred: (ItemEntity) -> Unit,
) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            IconButton(onClick = { onToggleRead(item) }) {
                Text(if (item.unread) "●" else "○")
            }
            IconButton(onClick = { onToggleStarred(item) }) {
                Icon(
                    imageVector = if (item.starred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = stringResource(R.string.star),
                )
            }
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
                },
            ) { Text(stringResource(R.string.open)) }
        }

        Column(Modifier.padding(horizontal = 16.dp)) {
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
                    loadDataWithBaseURL(null, item.body, "text/html", "UTF-8", null)
                }
            },
        )
    }
}

@Composable
private fun scopeLabel(scope: Scope): String = when (scope) {
    Scope.All -> stringResource(R.string.all_articles)
    Scope.Starred -> stringResource(R.string.starred)
    is Scope.Feed -> stringResource(R.string.feed)
}
