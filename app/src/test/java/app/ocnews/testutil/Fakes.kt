package app.ocnews.testutil

import app.ocnews.data.local.FeedDao
import app.ocnews.data.local.FeedEntity
import app.ocnews.data.local.FolderDao
import app.ocnews.data.local.FolderEntity
import app.ocnews.data.local.ItemDao
import app.ocnews.data.local.ItemBodyRow
import app.ocnews.data.local.ItemEntity
import app.ocnews.data.local.ItemSummary
import app.ocnews.data.remote.NewsApi
import app.ocnews.data.remote.dto.CreateFeedRequest
import app.ocnews.data.remote.dto.CreateFolderRequest
import app.ocnews.data.remote.dto.DiscoverResponse
import app.ocnews.data.remote.dto.FeedsResponse
import app.ocnews.data.remote.dto.FoldersResponse
import app.ocnews.data.remote.dto.FullItemResponse
import app.ocnews.data.remote.dto.ItemsResponse
import app.ocnews.data.remote.dto.MarkMultipleRequest
import app.ocnews.data.remote.dto.MoveFeedRequest
import app.ocnews.data.remote.dto.RenameFeedRequest
import app.ocnews.data.remote.dto.RenameFolderRequest
import app.ocnews.data.remote.dto.UserResponse
import app.ocnews.data.remote.dto.VersionResponse
import app.ocnews.data.session.Session
import app.ocnews.data.session.SessionStore
import app.ocnews.data.settings.AppSettings
import app.ocnews.data.settings.FeedListStyle
import app.ocnews.data.settings.ImageCacheMode
import app.ocnews.data.settings.SearchScope
import app.ocnews.data.settings.SettingsStore
import app.ocnews.data.settings.SortOrder
import app.ocnews.data.settings.SwipeAction
import app.ocnews.data.settings.TextSize
import app.ocnews.data.settings.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

fun item(
    id: Long,
    unread: Boolean,
    starred: Boolean = false,
    unreadPending: Boolean = false,
    starredPending: Boolean = false,
    lastModified: Long = id,
) = ItemEntity(
    id = id,
    guid = "g$id",
    guidHash = "h$id",
    url = "u$id",
    title = "t$id",
    author = "",
    pubDate = id,
    body = "",
    enclosureMime = null,
    enclosureLink = null,
    mediaThumbnail = null,
    mediaDescription = null,
    feedId = 1,
    unread = unread,
    starred = starred,
    filtered = false,
    rtl = false,
    lastModified = lastModified,
    fingerprint = "f$id",
    unreadPending = unreadPending,
    starredPending = starredPending,
)

fun summary(
    id: Long,
    unread: Boolean,
    starred: Boolean = false,
    title: String = "t$id",
    feedId: Long = 1,
    thumbnailUrl: String? = null,
) = ItemSummary(
    id = id,
    url = "u$id",
    title = title,
    author = "",
    pubDate = id,
    feedId = feedId,
    unread = unread,
    starred = starred,
    rtl = false,
    mediaThumbnail = null,
    thumbnailUrl = thumbnailUrl,
)

fun feed(
    id: Long,
    folderId: Long? = null,
    unreadCount: Long = 0,
) = FeedEntity(
    id = id,
    url = "http://feed$id",
    title = "feed$id",
    faviconLink = "",
    added = 0,
    nextUpdateTime = 0,
    folderId = folderId,
    unreadCount = unreadCount,
    ordering = 0,
    link = "",
    pinned = false,
    updateErrorCount = 0,
    lastUpdateError = null,
    urlHash = "",
    isPodcast = false,
)

class FakeSessionStore : SessionStore {
    private val state = MutableStateFlow<Session?>(null)
    override val session: Flow<Session?> = state
    override suspend fun save(session: Session) { state.value = session }
    override suspend fun clear() { state.value = null }
}

class FakeSettingsStore : SettingsStore {
    private val state = MutableStateFlow(AppSettings())
    override val settings: Flow<AppSettings> = state
    override suspend fun setTheme(mode: ThemeMode) { state.value = state.value.copy(theme = mode) }
    override suspend fun setSyncInterval(minutes: Int) {
        state.value = state.value.copy(syncIntervalMinutes = minutes)
    }
    override suspend fun setSyncOnStart(value: Boolean) { state.value = state.value.copy(syncOnStart = value) }
    override suspend fun setOnlyUnread(value: Boolean) { state.value = state.value.copy(onlyUnread = value) }
    override suspend fun setSwipeRight(action: SwipeAction) { state.value = state.value.copy(swipeRight = action) }
    override suspend fun setSwipeLeft(action: SwipeAction) { state.value = state.value.copy(swipeLeft = action) }
    override suspend fun setSortOrder(order: SortOrder) { state.value = state.value.copy(sortOrder = order) }
    override suspend fun setSearchIn(scope: SearchScope) { state.value = state.value.copy(searchIn = scope) }
    override suspend fun setFeedListStyle(style: FeedListStyle) { state.value = state.value.copy(feedListStyle = style) }
    override suspend fun setTextSize(size: TextSize) { state.value = state.value.copy(textSize = size) }
    override suspend fun setOledBackground(value: Boolean) {
        state.value = state.value.copy(oledBackground = value)
    }
    override suspend fun setNavigateWithVolumeButtons(value: Boolean) {
        state.value = state.value.copy(navigateWithVolumeButtons = value)
    }
    override suspend fun setImageCacheMode(mode: ImageCacheMode) {
        state.value = state.value.copy(imageCacheMode = mode)
    }
    override suspend fun setMaxCacheSizeMb(mb: Int) {
        state.value = state.value.copy(maxCacheSizeMb = mb)
    }
    override suspend fun setHideRelatedSections(value: Boolean) {
        state.value = state.value.copy(hideRelatedSections = value)
    }
}

class FakeItemDao : ItemDao {
    val items = mutableListOf<ItemEntity>()
    val clearUnread = mutableListOf<List<Long>>()
    val clearStarred = mutableListOf<List<Long>>()
    val setUnreadCalls = mutableListOf<Pair<Long, Boolean>>()
    val setStarredCalls = mutableListOf<Pair<Long, Boolean>>()

    // Mirrors the DAO's REPLACE strategy.
    override suspend fun upsertAll(items: List<ItemEntity>) {
        for (item in items) {
            val idx = this.items.indexOfFirst { it.id == item.id }
            if (idx >= 0) this.items[idx] = item else this.items.add(item)
        }
    }
    override fun getAllSummaries(): Flow<List<ItemSummary>> = emptyFlow()
    override fun getUnreadSummaries(): Flow<List<ItemSummary>> = emptyFlow()
    override fun getStarredSummaries(): Flow<List<ItemSummary>> = emptyFlow()
    override fun getByFeedSummaries(feedId: Long): Flow<List<ItemSummary>> = emptyFlow()
    override suspend fun getUnreadIds(): List<Long> = items.filter { it.unread }.map { it.id }
    override suspend fun getUnreadImageUrls(): List<String> =
        items.filter { it.unread }
            .mapNotNull { it.mediaThumbnail ?: it.thumbnailUrl }
            .filter { it.isNotBlank() }
    override suspend fun setFullBody(id: Long, body: String?) {
        val idx = items.indexOfFirst { it.id == id }
        if (idx >= 0) items[idx] = items[idx].copy(fullBody = body)
    }
    override suspend fun clearFullBodies() {
        for (i in items.indices) items[i] = items[i].copy(fullBody = null)
    }
    override suspend fun getFullBodies(ids: List<Long>): List<app.ocnews.data.local.ItemFullBody> =
        items.filter { it.id in ids && it.fullBody != null }
            .map { app.ocnews.data.local.ItemFullBody(it.id, it.fullBody!!) }
    override suspend fun searchBodyIds(query: String): List<Long> =
        items.filter { it.body.contains(query, ignoreCase = true) }.map { it.id }
    override suspend fun getItemsMissingThumbnail(limit: Int): List<ItemBodyRow> =
        items.filter { it.thumbnailUrl == null && it.body.contains("<img") }
            .take(limit)
            .map { ItemBodyRow(it.id, it.body) }
    override suspend fun setThumbnailUrl(id: Long, thumbnailUrl: String?) {
        val idx = items.indexOfFirst { it.id == id }
        if (idx >= 0) items[idx] = items[idx].copy(thumbnailUrl = thumbnailUrl)
    }
    override suspend fun getById(id: Long): ItemEntity? = items.firstOrNull { it.id == id }
    override suspend fun getHighestLastModified(): Long? = items.maxOfOrNull { it.lastModified }
    override fun observeUnreadCount(): Flow<Int> = kotlinx.coroutines.flow.flowOf(items.count { it.unread })
    override fun observeStarredCount(): Flow<Int> = kotlinx.coroutines.flow.flowOf(items.count { it.starred })
    override suspend fun setUnread(id: Long, unread: Boolean) { setUnreadCalls.add(id to unread) }
    override suspend fun markAllRead() {
        for (i in items.indices) items[i] = items[i].copy(unread = false, unreadPending = false)
    }
    override suspend fun setStarred(id: Long, starred: Boolean) { setStarredCalls.add(id to starred) }
    override suspend fun getPendingUnread(): List<ItemEntity> = items.filter { it.unreadPending }
    override suspend fun getPendingStarred(): List<ItemEntity> = items.filter { it.starredPending }
    override suspend fun clearUnreadPending(ids: List<Long>) { clearUnread.add(ids) }
    override suspend fun clearStarredPending(ids: List<Long>) { clearStarred.add(ids) }
    override suspend fun clear() { items.clear() }
}

class FakeImagePrefetcher : app.ocnews.data.repo.ImagePrefetcher {
    val urls = mutableListOf<String>()
    override suspend fun prefetch(url: String) {
        urls.add(url)
    }
}

class FakeFolderDao : FolderDao {
    val folders = mutableListOf<FolderEntity>()
    override suspend fun upsertAll(folders: List<FolderEntity>) { this.folders.addAll(folders) }
    override fun getAll(): Flow<List<FolderEntity>> = emptyFlow()
    override suspend fun getById(id: Long): FolderEntity? = null
    override suspend fun getByName(name: String): FolderEntity? = folders.firstOrNull { it.name == name }
    override suspend fun rename(id: Long, name: String) {
        folders.replaceAll { if (it.id == id) it.copy(name = name) else it }
    }
    override suspend fun deleteById(id: Long) {}
    override suspend fun clear() { folders.clear() }
}

class FakeFeedDao : FeedDao {
    val feeds = mutableListOf<FeedEntity>()
    override suspend fun upsertAll(feeds: List<FeedEntity>) { this.feeds.addAll(feeds) }
    override fun getAll(): Flow<List<FeedEntity>> = emptyFlow()
    override fun getByFolder(folderId: Long): Flow<List<FeedEntity>> = emptyFlow()
    override fun getRootFeeds(): Flow<List<FeedEntity>> = emptyFlow()
    override suspend fun getById(id: Long): FeedEntity? = null
    override suspend fun rename(id: Long, title: String) {}
    override suspend fun move(id: Long, folderId: Long?) {}
    override suspend fun deleteById(id: Long) {}
    override suspend fun clear() { feeds.clear() }
}

class FakeNewsApi(
    var versionResponse: String = "28.4.0",
    var foldersResponse: FoldersResponse = FoldersResponse(emptyList()),
    var feedsResponse: FeedsResponse = FeedsResponse(emptyList()),
    var itemsResponse: ItemsResponse = ItemsResponse(emptyList()),
    var itemsUpdatedResponse: ItemsResponse = ItemsResponse(emptyList()),
    var createFolderResponse: FoldersResponse = FoldersResponse(emptyList()),
    var createFeedResponse: FeedsResponse = FeedsResponse(emptyList()),
    var discoverResponse: DiscoverResponse = DiscoverResponse(emptyList()),
    var createFeedErrorCode: Int? = null,
) : NewsApi {
    val markUnreadCalls = mutableListOf<List<Long>>()
    val markReadCalls = mutableListOf<List<Long>>()
    val markStarCalls = mutableListOf<List<Long>>()
    val markUnstarCalls = mutableListOf<List<Long>>()
    val itemsCalls = mutableListOf<Triple<Int, Boolean, Int>>() // type, getRead, batchSize
    val itemsUpdatedLastModified = mutableListOf<Long>()
    val createFolderCalls = mutableListOf<String>()
    val createFeedCalls = mutableListOf<Pair<String, Long?>>()
    val deleteFolderCalls = mutableListOf<Long>()
    val deleteFeedCalls = mutableListOf<Long>()
    val renameFeedCalls = mutableListOf<Pair<Long, String>>()
    val renameFolderCalls = mutableListOf<Pair<Long, String>>()
    val moveFeedCalls = mutableListOf<Pair<Long, Long>>()
    var markAllReadCalls = 0

    override suspend fun version() = VersionResponse(versionResponse)
    override suspend fun user() = UserResponse("u", "User")
    override suspend fun folders() = foldersResponse
    override suspend fun createFolder(body: CreateFolderRequest): FoldersResponse {
        createFolderCalls.add(body.name)
        return createFolderResponse
    }
    override suspend fun renameFolder(id: Long, body: RenameFolderRequest) { renameFolderCalls.add(id to body.name) }
    override suspend fun deleteFolder(id: Long) { deleteFolderCalls.add(id) }
    override suspend fun markFolderRead(id: Long) {}
    override suspend fun feeds() = feedsResponse
    override suspend fun createFeed(body: CreateFeedRequest): FeedsResponse {
        createFeedCalls.add(body.url to body.folderId)
        createFeedErrorCode?.let { code ->
            throw HttpException(
                Response.error<FeedsResponse>(code, "".toResponseBody("application/json".toMediaType()))
            )
        }
        return createFeedResponse
    }
    override suspend fun discover(url: String): DiscoverResponse = discoverResponse
    override suspend fun deleteFeed(id: Long) { deleteFeedCalls.add(id) }
    override suspend fun moveFeed(id: Long, body: MoveFeedRequest) { moveFeedCalls.add(id to body.folderId) }
    override suspend fun renameFeed(id: Long, body: RenameFeedRequest) { renameFeedCalls.add(id to body.feedTitle) }

    override suspend fun items(
        type: Int,
        id: Long?,
        getRead: Boolean,
        batchSize: Int,
        offset: Long?,
        oldestFirst: Boolean,
    ): ItemsResponse {
        itemsCalls.add(Triple(type, getRead, batchSize))
        return itemsResponse
    }

    override suspend fun itemsUpdated(lastModified: Long, type: Int, id: Long): ItemsResponse {
        itemsUpdatedLastModified.add(lastModified)
        return itemsUpdatedResponse
    }

    override suspend fun itemFull(id: Long): FullItemResponse = FullItemResponse("full body for $id")

    override suspend fun markRead(id: Long) {}
    override suspend fun markUnread(id: Long) {}
    override suspend fun markStar(id: Long) {}
    override suspend fun markUnstar(id: Long) {}
    override suspend fun markReadMultiple(body: MarkMultipleRequest) { markReadCalls.add(body.items) }
    override suspend fun markUnreadMultiple(body: MarkMultipleRequest) { markUnreadCalls.add(body.items) }
    override suspend fun markStarMultiple(body: MarkMultipleRequest) { markStarCalls.add(body.items) }
    override suspend fun markUnstarMultiple(body: MarkMultipleRequest) { markUnstarCalls.add(body.items) }
    override suspend fun markAllRead() { markAllReadCalls++ }
}
