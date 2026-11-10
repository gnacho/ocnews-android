package app.ocnews.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.ocnews.data.local.FeedEntity
import app.ocnews.data.local.FolderEntity
import app.ocnews.data.local.ItemEntity
import app.ocnews.data.local.ItemSummary
import app.ocnews.data.repo.ImagePrefetcher
import app.ocnews.data.repo.NetworkStatus
import app.ocnews.data.repo.NewsRepository
import app.ocnews.data.settings.ImageCacheMode
import app.ocnews.data.settings.SearchScope
import app.ocnews.data.settings.SettingsStore
import app.ocnews.data.settings.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DownloadKind { ARTICLES, IMAGES }

data class DownloadProgress(val kind: DownloadKind, val done: Int, val total: Int)

enum class DownloadBlocked { DISABLED, WIFI_ONLY }

data class DownloadFinished(
    val kind: DownloadKind,
    val count: Int,
    val blocked: DownloadBlocked? = null,
)

sealed interface Scope {
    data object All : Scope
    data object Starred : Scope
    data class Feed(val id: Long) : Scope
    data class Folder(val id: Long) : Scope
}

// Pure filter, unit-testable without coroutines. Body matches arrive as ids
// resolved by SQL, so the list never holds article HTML in memory.
fun filterItems(
    items: List<ItemSummary>,
    feeds: List<FeedEntity>,
    scope: Scope,
    onlyUnread: Boolean,
    query: String = "",
    sort: SortOrder = SortOrder.NEWEST_FIRST,
    searchIn: SearchScope = SearchScope.TITLE,
    contentMatchIds: Set<Long> = emptySet(),
    keepReadIds: Set<Long> = emptySet(),
): List<ItemSummary> {
    val scoped = when (scope) {
        Scope.All -> items
        Scope.Starred -> items.filter { it.starred }
        is Scope.Feed -> items.filter { it.feedId == scope.id }
        is Scope.Folder -> {
            val feedIds = feeds.filter { it.folderId == scope.id }.map { it.id }.toSet()
            items.filter { it.feedId in feedIds }
        }
    }
    // Articles read during this session stay in the list until the next refresh,
    // so going back from the reader does not make them vanish.
    val unreadFiltered = if (onlyUnread) {
        scoped.filter { it.unread || it.id in keepReadIds }
    } else {
        scoped
    }
    val q = query.trim()
    val searched = if (q.isEmpty()) unreadFiltered else unreadFiltered.filter {
        val inTitle = it.title.contains(q, ignoreCase = true)
        val inBody = it.id in contentMatchIds
        when (searchIn) {
            SearchScope.TITLE -> inTitle
            SearchScope.CONTENT -> inBody
            SearchScope.BOTH -> inTitle || inBody
        }
    }
    return when (sort) {
        SortOrder.NEWEST_FIRST -> searched
        SortOrder.OLDEST_FIRST -> searched.reversed()
    }
}

class ListViewModel(
    private val repository: NewsRepository,
    private val settingsStore: SettingsStore,
    private val imagePrefetcher: ImagePrefetcher = ImagePrefetcher { },
    private val networkStatus: NetworkStatus = NetworkStatus { true },
) : ViewModel() {
    private val scope = MutableStateFlow<Scope>(Scope.All)
    private val _searchQuery = MutableStateFlow("")
    private val _fullContent = MutableStateFlow<Map<Long, String>>(emptyMap())
    // True when the server could not provide the full text (Nextcloud has no such endpoint).
    private val _fullTextFailed = MutableStateFlow(false)
    private val _itemDetails = MutableStateFlow<Map<Long, ItemEntity>>(emptyMap())
    private val _syncing = MutableStateFlow(false)
    private val _download = MutableStateFlow<DownloadProgress?>(null)
    private val _finished = MutableStateFlow<DownloadFinished?>(null)
    private val _pendingImageDownload = MutableStateFlow(false)
    // Articles the user explicitly kept unread during this reader session.
    private val keepUnread = mutableSetOf<Long>()
    private val autoHandled = mutableSetOf<Long>()
    // Read during this session: kept in the list until a refresh happens.
    private val readThisSession = MutableStateFlow<Set<Long>>(emptySet())

    private data class UiFilter(
        val onlyUnread: Boolean,
        val query: String,
        val sort: SortOrder,
        val searchIn: SearchScope,
        val keepRead: Set<Long>,
    )

    init {
        viewModelScope.launch {
            // Rows stored before Room v2 have no thumbnailUrl yet.
            runCatching { repository.backfillThumbnails() }
        }
        viewModelScope.launch {
            val s = settingsStore.settings.first()
            if (s.syncOnStart) sync()
        }
    }

    val scopeState: StateFlow<Scope> = scope
    val onlyUnreadState: StateFlow<Boolean> = settingsStore.settings
        .map { it.onlyUnread }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val searchQuery: StateFlow<String> = _searchQuery
    val fullContent: StateFlow<Map<Long, String>> = _fullContent
    val fullTextFailed: StateFlow<Boolean> = _fullTextFailed
    // Feeds configured to fetch the server-side full text when an article opens.
    val fullTextFeedIds: StateFlow<Set<Long>> = settingsStore.settings
        .map { it.fullTextFeedIds }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val itemDetails: StateFlow<Map<Long, ItemEntity>> = _itemDetails
    val syncing: StateFlow<Boolean> = _syncing
    val download: StateFlow<DownloadProgress?> = _download
    val finished: StateFlow<DownloadFinished?> = _finished
    // True when the image download is waiting for the user to confirm off Wi-Fi.
    val pendingImageDownload: StateFlow<Boolean> = _pendingImageDownload

    val folders: StateFlow<List<FolderEntity>> = repository.observeFolders()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val feeds: StateFlow<List<FeedEntity>> = repository.observeFeeds()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val unreadCount: StateFlow<Int> = repository.observeUnreadCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val starredCount: StateFlow<Int> = repository.observeStarredCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // Ids whose body matches the query, resolved in SQL with a small debounce so
    // typing does not hit the database on every keystroke.
    private val contentMatchIds: Flow<Set<Long>> = combine(
        _searchQuery,
        settingsStore.settings,
    ) { q, s -> q.trim() to s.searchIn }
        .debounce(250)
        .flatMapLatest { (q, searchIn) ->
            flow {
                val ids = if (q.isEmpty() || searchIn == SearchScope.TITLE) {
                    emptySet()
                } else {
                    runCatching { repository.searchBodyIds(q).toSet() }.getOrDefault(emptySet())
                }
                emit(ids)
            }
        }

    val items: StateFlow<List<ItemSummary>> = combine(
        repository.observeItems(),
        repository.observeFeeds(),
        scope,
        combine(onlyUnreadState, _searchQuery, settingsStore.settings, readThisSession) { ou, q, s, keepRead ->
            UiFilter(ou, q, s.sortOrder, s.searchIn, keepRead)
        },
        contentMatchIds,
    ) { list, feeds, sc, f, matches ->
        filterItems(
            list,
            feeds,
            sc,
            f.onlyUnread,
            f.query,
            f.sort,
            f.searchIn,
            matches,
            f.keepRead,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun selectScope(s: Scope) {
        scope.value = s
    }

    fun toggleOnlyUnread() {
        viewModelScope.launch { settingsStore.setOnlyUnread(!onlyUnreadState.value) }
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun fetchFullContent(itemId: Long) {
        viewModelScope.launch {
            try {
                val body = repository.fetchFullContent(itemId)
                if (body != null) {
                    _fullContent.update { it + (itemId to body) }
                } else {
                    _fullTextFailed.value = true
                }
            } catch (e: Exception) {
                println("OCNEWS fetch full failed: ${e.message}")
                _fullTextFailed.value = true
            }
        }
    }

    fun clearFullTextFailed() {
        _fullTextFailed.value = false
    }

    // Loads the full row (with body) for the reader, once per article.
    fun loadItemDetail(id: Long) {
        if (_itemDetails.value.containsKey(id)) return
        viewModelScope.launch {
            runCatching { repository.itemById(id) }.getOrNull()?.let { item ->
                // The row may be read before the pending write lands, so apply
                // what this session already decided for the article.
                val unread = when {
                    id in keepUnread -> true
                    id in readThisSession.value -> false
                    else -> item.unread
                }
                _itemDetails.update { it + (id to item.copy(unread = unread)) }
            }
        }
    }

    // Opening an article marks it read, unless the user just marked it unread.
    fun autoMarkRead(id: Long) {
        if (id in keepUnread || id in autoHandled) return
        autoHandled += id
        readThisSession.update { it + id }
        viewModelScope.launch { runCatching { repository.setUnread(id, false) } }
        _itemDetails.update { map ->
            map[id]?.let { map + (id to it.copy(unread = false)) } ?: map
        }
    }

    // Forgets the per-session decisions when the reader is closed.
    fun resetReaderSession() {
        keepUnread.clear()
        autoHandled.clear()
    }

    fun toggleRead(id: Long, unread: Boolean) {
        val target = !unread
        if (target) {
            keepUnread += id
            readThisSession.update { it - id }
        } else {
            keepUnread -= id
            readThisSession.update { it + id }
        }
        viewModelScope.launch { repository.setUnread(id, target) }
        _itemDetails.update { map ->
            map[id]?.let { map + (id to it.copy(unread = target)) } ?: map
        }
    }

    fun toggleStarred(id: Long, starred: Boolean) {
        viewModelScope.launch { repository.setStarred(id, !starred) }
        _itemDetails.update { map ->
            map[id]?.let { map + (id to it.copy(starred = !starred)) } ?: map
        }
    }

    // Downloads the server-side full text of every unread article, so it can be
    // read offline. Runs one request at a time to keep the server happy.
    fun downloadUnreadArticles() {
        if (_download.value != null) return
        viewModelScope.launch {
            val ids = runCatching { repository.unreadIds() }.getOrDefault(emptyList())
            if (ids.isEmpty()) {
                _finished.value = DownloadFinished(DownloadKind.ARTICLES, 0)
                return@launch
            }
            _download.value = DownloadProgress(DownloadKind.ARTICLES, 0, ids.size)
            var done = 0
            for (id in ids) {
                runCatching { repository.fetchFullContent(id) }
                done++
                _download.value = DownloadProgress(DownloadKind.ARTICLES, done, ids.size)
            }
            _download.value = null
            _finished.value = DownloadFinished(DownloadKind.ARTICLES, done)
        }
    }

    // Prefetches the hero image of every unread article into the disk cache,
    // honouring the image cache policy from the settings.
    fun downloadUnreadImages() {
        if (_download.value != null || _pendingImageDownload.value) return
        viewModelScope.launch {
            val mode = runCatching { settingsStore.settings.first().imageCacheMode }
                .getOrDefault(ImageCacheMode.WIFI_AND_MOBILE)
            val onWifi = runCatching { networkStatus.isOnWifi() }.getOrDefault(true)
            when {
                mode == ImageCacheMode.NEVER ->
                    _finished.value = DownloadFinished(DownloadKind.IMAGES, 0, DownloadBlocked.DISABLED)
                mode == ImageCacheMode.WIFI_ONLY && !onWifi ->
                    _finished.value = DownloadFinished(DownloadKind.IMAGES, 0, DownloadBlocked.WIFI_ONLY)
                mode == ImageCacheMode.ASK && !onWifi -> _pendingImageDownload.value = true
                else -> runImageDownload()
            }
        }
    }

    fun confirmImageDownload() {
        _pendingImageDownload.value = false
        viewModelScope.launch { if (_download.value == null) runImageDownload() }
    }

    fun cancelImageDownload() {
        _pendingImageDownload.value = false
    }

    private suspend fun runImageDownload() {
        val urls = runCatching { repository.unreadImageUrls() }.getOrDefault(emptyList())
        if (urls.isEmpty()) {
            _finished.value = DownloadFinished(DownloadKind.IMAGES, 0)
            return
        }
        _download.value = DownloadProgress(DownloadKind.IMAGES, 0, urls.size)
        var done = 0
        for (url in urls) {
            runCatching { imagePrefetcher.prefetch(url) }
            done++
            _download.value = DownloadProgress(DownloadKind.IMAGES, done, urls.size)
        }
        _download.value = null
        _finished.value = DownloadFinished(DownloadKind.IMAGES, done)
    }

    fun clearFinished() {
        _finished.value = null
    }

    // Applies the per-feed settings dialog: folder on the server, full text locally.
    fun saveFeedSettings(feedId: Long, folderId: Long?, fullText: Boolean) {
        viewModelScope.launch {
            runCatching { repository.moveFeed(feedId, folderId) }
            runCatching { settingsStore.setFullTextFeed(feedId, fullText) }
        }
    }

    fun sync() {
        viewModelScope.launch {
            _syncing.value = true
            val startedAt = System.currentTimeMillis()
            try {
                repository.sync()
            } catch (e: Exception) {
                // Network/API failures must not crash the app; the list keeps cached data.
                println("OCNEWS sync failed: ${e.message}")
            } finally {
                // After a refresh the articles read this session follow the
                // filter again, so they leave the list.
                readThisSession.value = emptySet()
                // Keep the refresh animation on screen for a moment even when the
                // server has nothing new, so the pull does not look ignored.
                val elapsed = System.currentTimeMillis() - startedAt
                if (elapsed < MIN_REFRESH_FEEDBACK_MS) {
                    delay(MIN_REFRESH_FEEDBACK_MS - elapsed)
                }
                _syncing.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch { repository.logout() }
    }

    fun markAllRead() {
        viewModelScope.launch {
            try {
                repository.markAllRead()
            } catch (e: Exception) {
                println("OCNEWS markAllRead failed: ${e.message}")
            }
        }
    }

    companion object {
        // Minimum time the pull-to-refresh indicator stays visible.
        private const val MIN_REFRESH_FEEDBACK_MS = 900L

        fun factory(
            repository: NewsRepository,
            settingsStore: SettingsStore,
            imagePrefetcher: ImagePrefetcher,
            networkStatus: NetworkStatus,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ListViewModel(repository, settingsStore, imagePrefetcher, networkStatus) as T
            }
    }
}
