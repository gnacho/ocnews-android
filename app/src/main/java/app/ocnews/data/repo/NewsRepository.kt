package app.ocnews.data.repo

import app.ocnews.R
import app.ocnews.data.local.FeedDao
import app.ocnews.data.local.FeedEntity
import app.ocnews.data.local.FolderDao
import app.ocnews.data.local.FolderEntity
import app.ocnews.data.local.ItemDao
import app.ocnews.data.local.ItemEntity
import app.ocnews.data.local.ItemSummary
import app.ocnews.data.remote.NewsApi
import app.ocnews.data.remote.dto.CreateFeedRequest
import app.ocnews.data.remote.dto.CreateFolderRequest
import app.ocnews.data.remote.dto.DiscoveredFeedDto
import app.ocnews.data.remote.dto.MoveFeedRequest
import app.ocnews.data.remote.dto.RenameFeedRequest
import app.ocnews.data.remote.dto.RenameFolderRequest
import app.ocnews.data.session.Session
import app.ocnews.data.session.SessionStore
import app.ocnews.data.toEntity
import app.ocnews.opml.OpmlParser
import app.ocnews.sync.SyncEngine
import app.ocnews.util.firstImageUrl
import app.ocnews.util.resolveUrl
import app.ocnews.util.unpackImageUrl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.HttpException

data class OpmlImportResult(
    val feedsAdded: Int,
    val foldersCreated: Int,
)

// Single concurrency boundary between UI (ViewModels) and data sources.
class NewsRepository(
    private val sessionStore: SessionStore,
    private val folderDao: FolderDao,
    private val feedDao: FeedDao,
    private val itemDao: ItemDao,
    private val apiFactory: (Session) -> NewsApi,
) {
    val session: Flow<Session?> = sessionStore.session

    sealed interface LoginResult {
        data object Success : LoginResult
        data class Error(val messageRes: Int, val args: List<Any> = emptyList()) : LoginResult
    }

    suspend fun login(serverUrl: String, username: String, token: String): LoginResult {
        val newSession = Session(serverUrl, username, token)
        return try {
            val version = apiFactory(newSession).version()
            if (version.version == "0") {
                LoginResult.Error(R.string.err_version_zero)
            } else {
                val previous = sessionStore.session.first()
                val changedAccount =
                    previous?.username != username || previous?.serverUrl != serverUrl
                sessionStore.save(newSession)
                if (changedAccount) clearLocalData()
                LoginResult.Success
            }
        } catch (e: HttpException) {
            if (e.code() == 401) {
                LoginResult.Error(R.string.err_invalid_credentials)
            } else {
                LoginResult.Error(R.string.err_server, listOf(e.code()))
            }
        } catch (e: Exception) {
            LoginResult.Error(R.string.err_login_failed)
        }
    }

    suspend fun logout() {
        sessionStore.clear()
        clearLocalData()
    }

    suspend fun sync() {
        val session = sessionStore.session.first() ?: return
        val engine = SyncEngine(
            apiFactory(session),
            folderDao,
            feedDao,
            itemDao,
            resolveUrl = { path -> resolveUrl(session.serverUrl, path) },
        )
        engine.pushPendingState()
        val hasItems = itemDao.getHighestLastModified() != null
        if (hasItems) engine.incrementalSync() else engine.initialSync()
    }

    suspend fun setUnread(itemId: Long, unread: Boolean) = itemDao.setUnread(itemId, unread)

    suspend fun setStarred(itemId: Long, starred: Boolean) = itemDao.setStarred(itemId, starred)

    fun observeFolders(): Flow<List<FolderEntity>> = folderDao.getAll()
    fun observeFeeds(): Flow<List<FeedEntity>> = feedDao.getAll()
    // Lightweight projections (no body) for the list, the unread and starred
    // views and per-feed views.
    fun observeItems(): Flow<List<ItemSummary>> = itemDao.getAllSummaries()
    fun observeUnread(): Flow<List<ItemSummary>> = itemDao.getUnreadSummaries()
    fun observeStarred(): Flow<List<ItemSummary>> = itemDao.getStarredSummaries()
    fun observeUnreadCount(): Flow<Int> = itemDao.observeUnreadCount()
    fun observeStarredCount(): Flow<Int> = itemDao.observeStarredCount()
    fun observeItemsByFeed(feedId: Long): Flow<List<ItemSummary>> = itemDao.getByFeedSummaries(feedId)

    // Full row (including the HTML body) for the reader.
    suspend fun itemById(id: Long): ItemEntity? = itemDao.getById(id)

    // Body search via SQL: returns matching ids without loading the HTML.
    suspend fun searchBodyIds(query: String): List<Long> = itemDao.searchBodyIds(query)

    // Fills thumbnailUrl for rows stored before the column existed (Room v2).
    suspend fun backfillThumbnails(batch: Int = 200) {
        while (true) {
            val rows = itemDao.getItemsMissingThumbnail(batch)
            if (rows.isEmpty()) return
            for (row in rows) {
                // Empty string means "checked, no image", so the row leaves the
                // pending set even when the body has no usable image.
                val url = firstImageUrl(row.body)?.let { unpackImageUrl(it) } ?: ""
                itemDao.setThumbnailUrl(row.id, url)
            }
        }
    }

    suspend fun addFeed(url: String, folderId: Long? = null) {
        val session = sessionStore.session.first() ?: return
        val resp = apiFactory(session).createFeed(CreateFeedRequest(url, folderId))
        feedDao.upsertAll(
            resp.feeds.map { it.toEntity().copy(faviconLink = resolveUrl(session.serverUrl, it.faviconLink)) }
        )
    }

    suspend fun discover(url: String): List<DiscoveredFeedDto> {
        return currentApi()?.discover(url)?.feeds ?: emptyList()
    }

    // Fetches the full article text (backend readability extraction). Returns null on failure.
    suspend fun unreadIds(): List<Long> = itemDao.getUnreadIds()

    suspend fun unreadImageUrls(): List<String> = itemDao.getUnreadImageUrls()

    suspend fun saveFullBody(itemId: Long, body: String?) = itemDao.setFullBody(itemId, body)

    // Drops the downloaded full text; the image disk cache is cleared separately.
    suspend fun clearOfflineCache() = itemDao.clearFullBodies()

    // Fetches the server-side full text (readability, cached by the server) and
    // stores it locally, so the article is readable offline afterwards.
    suspend fun fetchFullContent(itemId: Long): String? {
        val api = currentApi() ?: return null
        val body = api.itemFull(itemId).body.takeIf { it.isNotBlank() } ?: return null
        itemDao.setFullBody(itemId, body)
        return body
    }

    suspend fun createFolder(name: String) {
        val api = currentApi() ?: return
        val resp = api.createFolder(CreateFolderRequest(name))
        folderDao.upsertAll(resp.folders.map { it.toEntity() })
    }

    suspend fun renameFolder(id: Long, name: String) {
        val api = currentApi() ?: return
        api.renameFolder(id, RenameFolderRequest(name))
        folderDao.rename(id, name)
    }

    suspend fun deleteFolder(id: Long) {
        val api = currentApi() ?: return
        api.deleteFolder(id)
        folderDao.deleteById(id)
    }

    suspend fun renameFeed(id: Long, title: String) {
        val api = currentApi() ?: return
        api.renameFeed(id, RenameFeedRequest(title))
        feedDao.rename(id, title)
    }

    suspend fun moveFeed(id: Long, folderId: Long?) {
        val api = currentApi() ?: return
        api.moveFeed(id, MoveFeedRequest(folderId ?: 0L))
        feedDao.move(id, folderId)
    }

    suspend fun deleteFeed(id: Long) {
        val api = currentApi() ?: return
        api.deleteFeed(id)
        feedDao.deleteById(id)
    }

    suspend fun markAllRead() {
        itemDao.markAllRead()
        currentApi()?.markAllRead()
    }

    suspend fun importOpml(opmlXml: String): OpmlImportResult {
        val api = currentApi() ?: return OpmlImportResult(0, 0)
        val entries = OpmlParser.parse(opmlXml)
        var feedsAdded = 0
        var foldersCreated = 0
        val knownFolders = mutableMapOf<String, Long>()

        for (entry in entries) {
            val folderId = entry.folderName?.let { name ->
                knownFolders.getOrPut(name) {
                    folderDao.getByName(name)?.id
                        ?: run {
                            val resp = api.createFolder(CreateFolderRequest(name))
                            foldersCreated++
                            val id = resp.folders.first().id
                            folderDao.upsertAll(resp.folders.map { it.toEntity() })
                            id
                        }
                }
            }
            api.createFeed(CreateFeedRequest(entry.feedUrl, folderId))
            feedsAdded++
        }
        return OpmlImportResult(feedsAdded, foldersCreated)
    }

    private suspend fun currentApi(): NewsApi? {
        val session = sessionStore.session.first() ?: return null
        return apiFactory(session)
    }

    private suspend fun clearLocalData() {
        folderDao.clear()
        feedDao.clear()
        itemDao.clear()
    }
}
