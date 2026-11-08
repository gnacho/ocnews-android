package app.ocnews.sync

import app.ocnews.data.remote.dto.FeedDto
import app.ocnews.data.remote.dto.FeedsResponse
import app.ocnews.data.remote.dto.FolderDto
import app.ocnews.data.remote.dto.FoldersResponse
import app.ocnews.data.remote.dto.ItemDto
import app.ocnews.data.remote.dto.ItemsResponse
import app.ocnews.testutil.FakeFeedDao
import app.ocnews.testutil.FakeFolderDao
import app.ocnews.testutil.FakeItemDao
import app.ocnews.testutil.FakeNewsApi
import app.ocnews.testutil.item
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

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

    @Test
    fun `incrementalSync refreshes folders and feeds`() = runTest {
        val folderDao = FakeFolderDao()
        val feedDao = FakeFeedDao()
        val itemDao = FakeItemDao().apply { items.add(item(1, unread = true, lastModified = 500)) }
        val api = FakeNewsApi(
            foldersResponse = FoldersResponse(listOf(FolderDto(1, "Tech"))),
            feedsResponse = FeedsResponse(listOf(FeedDto(10, "http://feed", "Feed", folderId = 1))),
        )

        SyncEngine(api, folderDao, feedDao, itemDao).incrementalSync()

        assertEquals(1, folderDao.folders.size)
        assertEquals("Tech", folderDao.folders.first().name)
        assertEquals(1, feedDao.feeds.size)
        assertEquals(10L, feedDao.feeds.first().id)
    }

    @Test
    fun `sync resolves relative favicon against server`() = runTest {
        val feedDao = FakeFeedDao()
        val api = FakeNewsApi(
            feedsResponse = FeedsResponse(
                listOf(FeedDto(10, "http://feed", "Feed", faviconLink = "/favicon/abc"))
            ),
        )

        SyncEngine(
            api,
            FakeFolderDao(),
            feedDao,
            FakeItemDao(),
            resolveUrl = { path -> "https://host" + path },
        ).initialSync()

        assertEquals("https://host/favicon/abc", feedDao.feeds.first().faviconLink)
    }
}
