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
    val unreadFiltered = if (onlyUnread) scoped.filter { it.unread } else scoped
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
    private val _itemDetails = MutableStateFlow<Map<Long, ItemEntity>>(emptyMap())
    private val _syncing = MutableStateFlow(false)
    private val _download = MutableStateFlow<DownloadProgress?>(null)
    private val _finished = MutableStateFlow<DownloadFinished?>(null)
    private val _pendingImageDownload = MutableStateFlow(false)

    private data class UiFilter(
        val onlyUnread: Boolean,
        val query: String,
        val sort: SortOrder,
        val searchIn: SearchScope,
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
        combine(onlyUnreadState, _searchQuery, settingsStore.settings) { ou, q, s ->
            UiFilter(ou, q, s.sortOrder, s.searchIn)
        },
        contentMatchIds,
    ) { list, feeds, sc, f, matches ->
        filterItems(list, feeds, sc, f.onlyUnread, f.query, f.sort, f.searchIn, matches)
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
                repository.fetchFullContent(itemId)?.let { body ->
                    _fullContent.update { it + (itemId to body) }
                }
            } catch (e: Exception) {
                println("OCNEWS fetch full failed: ${e.message}")
            }
        }
    }

    // Loads the full row (with body) for the reader, once per article.
    fun loadItemDetail(id: Long) {
        if (_itemDetails.value.containsKey(id)) return
        viewModelScope.launch {
            runCatching { repository.itemById(id) }.getOrNull()?.let { item ->
                _itemDetails.update { it + (id to item) }
            }
        }
    }

    fun toggleRead(id: Long, unread: Boolean) {
        viewModelScope.launch { repository.setUnread(id, !unread) }
        _itemDetails.update { map ->
            map[id]?.let { map + (id to it.copy(unread = !unread)) } ?: map
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

    fun sync() {
        viewModelScope.launch {
            _syncing.value = true
            try {
                repository.sync()
            } catch (e: Exception) {
                // Network/API failures must not crash the app; the list keeps cached data.
                println("OCNEWS sync failed: ${e.message}")
            } finally {
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
