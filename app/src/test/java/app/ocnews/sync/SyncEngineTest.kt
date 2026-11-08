package app.ocnews.sync

import app.ocnews.data.local.FeedDao
import app.ocnews.data.local.FeedEntity
import app.ocnews.data.local.FolderDao
import app.ocnews.data.local.FolderEntity
import app.ocnews.data.local.ItemDao
import app.ocnews.data.local.ItemEntity
import app.ocnews.data.remote.NewsApi
import app.ocnews.data.remote.dto.CreateFeedRequest
import app.ocnews.data.remote.dto.CreateFolderRequest
import app.ocnews.data.remote.dto.DiscoverResponse
import app.ocnews.data.remote.dto.FeedDto
import app.ocnews.data.remote.dto.FeedsResponse
import app.ocnews.data.remote.dto.FolderDto
import app.ocnews.data.remote.dto.FoldersResponse
import app.ocnews.data.remote.dto.ItemDto
import app.ocnews.data.remote.dto.ItemsResponse
import app.ocnews.data.remote.dto.MarkMultipleRequest
import app.ocnews.data.remote.dto.MoveFeedRequest
import app.ocnews.data.remote.dto.RenameFeedRequest
import app.ocnews.data.remote.dto.RenameFolderRequest
import app.ocnews.data.remote.dto.UserResponse
import app.ocnews.data.remote.dto.VersionResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private fun item(
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

private class FakeItemDao : ItemDao {
    val items = mutableListOf<ItemEntity>()
    val clearUnread = mutableListOf<List<Long>>()
    val clearStarred = mutableListOf<List<Long>>()

    override suspend fun upsertAll(items: List<ItemEntity>) { this.items.addAll(items) }
    override fun getAll(): Flow<List<ItemEntity>> = emptyFlow()
    override fun getUnread(): Flow<List<ItemEntity>> = emptyFlow()
    override fun getStarred(): Flow<List<ItemEntity>> = emptyFlow()
    override fun getByFeed(feedId: Long): Flow<List<ItemEntity>> = emptyFlow()
    override suspend fun getById(id: Long): ItemEntity? = items.firstOrNull { it.id == id }
    override suspend fun getHighestLastModified(): Long? = items.maxOfOrNull { it.lastModified }
    override suspend fun setUnread(id: Long, unread: Boolean) {}
    override suspend fun setStarred(id: Long, starred: Boolean) {}
    override suspend fun getPendingUnread(): List<ItemEntity> = items.filter { it.unreadPending }
    override suspend fun getPendingStarred(): List<ItemEntity> = items.filter { it.starredPending }
    override suspend fun clearUnreadPending(ids: List<Long>) { clearUnread.add(ids) }
    override suspend fun clearStarredPending(ids: List<Long>) { clearStarred.add(ids) }
    override suspend fun clear() { items.clear() }
}

private class FakeFolderDao : FolderDao {
    val folders = mutableListOf<FolderEntity>()
    override suspend fun upsertAll(folders: List<FolderEntity>) { this.folders.addAll(folders) }
    override fun getAll(): Flow<List<FolderEntity>> = emptyFlow()
    override suspend fun getById(id: Long): FolderEntity? = null
    override suspend fun deleteById(id: Long) {}
    override suspend fun clear() { folders.clear() }
}

private class FakeFeedDao : FeedDao {
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

private class FakeNewsApi(
    var foldersResponse: FoldersResponse = FoldersResponse(emptyList()),
    var feedsResponse: FeedsResponse = FeedsResponse(emptyList()),
    var itemsResponse: ItemsResponse = ItemsResponse(emptyList()),
    var itemsUpdatedResponse: ItemsResponse = ItemsResponse(emptyList()),
) : NewsApi {
    val markUnreadCalls = mutableListOf<List<Long>>()
    val markReadCalls = mutableListOf<List<Long>>()
    val markStarCalls = mutableListOf<List<Long>>()
    val markUnstarCalls = mutableListOf<List<Long>>()
    val itemsCalls = mutableListOf<Triple<Int, Boolean, Int>>() // type, getRead, batchSize
    val itemsUpdatedLastModified = mutableListOf<Long>()

    override suspend fun version() = VersionResponse("28.4.0")
    override suspend fun user() = UserResponse("u", "User")
    override suspend fun folders() = foldersResponse
    override suspend fun createFolder(body: CreateFolderRequest) = FoldersResponse(emptyList())
    override suspend fun renameFolder(id: Long, body: RenameFolderRequest) {}
    override suspend fun deleteFolder(id: Long) {}
    override suspend fun markFolderRead(id: Long) {}
    override suspend fun feeds() = feedsResponse
    override suspend fun createFeed(body: CreateFeedRequest) = FeedsResponse(emptyList())
    override suspend fun discover(url: String) = DiscoverResponse(emptyList())
    override suspend fun deleteFeed(id: Long) {}
    override suspend fun moveFeed(id: Long, body: MoveFeedRequest) {}
    override suspend fun renameFeed(id: Long, body: RenameFeedRequest) {}

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

    override suspend fun markRead(id: Long) {}
    override suspend fun markUnread(id: Long) {}
    override suspend fun markStar(id: Long) {}
    override suspend fun markUnstar(id: Long) {}
    override suspend fun markReadMultiple(body: MarkMultipleRequest) { markReadCalls.add(body.items) }
    override suspend fun markUnreadMultiple(body: MarkMultipleRequest) { markUnreadCalls.add(body.items) }
    override suspend fun markStarMultiple(body: MarkMultipleRequest) { markStarCalls.add(body.items) }
    override suspend fun markUnstarMultiple(body: MarkMultipleRequest) { markUnstarCalls.add(body.items) }
    override suspend fun markAllRead() {}
}

class SyncEngineTest {

    @Test
    fun `pushPendingState partitions and clears flags`() = runTest {
        val itemDao = FakeItemDao().apply {
            items.addAll(
                listOf(
                    item(1, unread = true, unreadPending = true),
                    item(2, unread = false, unreadPending = true),
                    item(3, unread = false, starred = true, starredPending = true),
                    item(4, unread = false, starred = false, starredPending = true),
                )
            )
        }
        val api = FakeNewsApi()
        SyncEngine(api, FakeFolderDao(), FakeFeedDao(), itemDao).pushPendingState()

        assertEquals(listOf(listOf(1L)), api.markUnreadCalls)
        assertEquals(listOf(listOf(2L)), api.markReadCalls)
        assertEquals(listOf(listOf(3L)), api.markStarCalls)
        assertEquals(listOf(listOf(4L)), api.markUnstarCalls)
        assertEquals(listOf(listOf(1L), listOf(2L)), itemDao.clearUnread)
        assertEquals(listOf(listOf(3L), listOf(4L)), itemDao.clearStarred)
    }

    @Test
    fun `pushPendingState sends nothing when no pending`() = runTest {
        val api = FakeNewsApi()
        SyncEngine(api, FakeFolderDao(), FakeFeedDao(), FakeItemDao()).pushPendingState()

        assertEquals(emptyList<List<Long>>(), api.markUnreadCalls)
        assertEquals(emptyList<List<Long>>(), api.markReadCalls)
        assertEquals(emptyList<List<Long>>(), api.markStarCalls)
        assertEquals(emptyList<List<Long>>(), api.markUnstarCalls)
    }

    @Test
    fun `initialSync upserts folders feeds and items`() = runTest {
        val folderDao = FakeFolderDao()
        val feedDao = FakeFeedDao()
        val itemDao = FakeItemDao()
        val api = FakeNewsApi(
            foldersResponse = FoldersResponse(listOf(FolderDto(1, "Tech"))),
            feedsResponse = FeedsResponse(listOf(FeedDto(10, "http://feed", "Feed", folderId = 1))),
            itemsResponse = ItemsResponse(listOf(ItemDto(100, title = "A", feedId = 10, unread = true))),
        )

        SyncEngine(api, folderDao, feedDao, itemDao).initialSync()

        assertEquals(1, folderDao.folders.size)
        assertEquals(1, feedDao.feeds.size)
        assertEquals(2, itemDao.items.size) // same item upserted twice (unread + starred)
        assertEquals(
            listOf(Triple(3, false, -1), Triple(2, true, -1)),
            api.itemsCalls,
        )
    }

    @Test
    fun `incrementalSync uses highest lastModified as watermark`() = runTest {
        val itemDao = FakeItemDao().apply {
            items.add(item(1, unread = true, starred = false, lastModified = 500))
            items.add(item(2, unread = false, starred = false, lastModified = 900))
        }
        val api = FakeNewsApi(
            itemsUpdatedResponse = ItemsResponse(listOf(ItemDto(3, title = "new", lastModified = 950)))
        )

        SyncEngine(api, FakeFolderDao(), FakeFeedDao(), itemDao).incrementalSync()

        assertEquals(listOf(900L), api.itemsUpdatedLastModified)
        assertEquals(3, itemDao.items.size)
    }
}
