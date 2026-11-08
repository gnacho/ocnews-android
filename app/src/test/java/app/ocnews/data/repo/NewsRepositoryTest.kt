package app.ocnews.data.repo

import app.ocnews.data.remote.dto.FeedDto
import app.ocnews.data.remote.dto.FeedsResponse
import app.ocnews.data.remote.dto.FolderDto
import app.ocnews.data.remote.dto.FoldersResponse
import app.ocnews.data.remote.dto.ItemDto
import app.ocnews.data.remote.dto.ItemsResponse
import app.ocnews.data.session.Session
import app.ocnews.testutil.FakeFeedDao
import app.ocnews.testutil.FakeFolderDao
import app.ocnews.testutil.FakeItemDao
import app.ocnews.testutil.FakeNewsApi
import app.ocnews.testutil.FakeSessionStore
import app.ocnews.testutil.item
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsRepositoryTest {

    private fun repo(
        sessionStore: FakeSessionStore = FakeSessionStore(),
        folderDao: FakeFolderDao = FakeFolderDao(),
        feedDao: FakeFeedDao = FakeFeedDao(),
        itemDao: FakeItemDao = FakeItemDao(),
        api: FakeNewsApi = FakeNewsApi(),
    ) = NewsRepository(
        sessionStore = sessionStore,
        folderDao = folderDao,
        feedDao = feedDao,
        itemDao = itemDao,
        apiFactory = { api },
    )

    @Test
    fun `login clears data on account change and preserves on same account`() = runTest {
        val sessionStore = FakeSessionStore()
        val itemDao = FakeItemDao().apply { items.add(item(1, unread = true)) }
        val repo = repo(sessionStore = sessionStore, itemDao = itemDao)

        // first login (no previous): treated as account change -> clears stale data
        assertEquals(NewsRepository.LoginResult.Success, repo.login("https://example.com", "alice", "token"))
        assertEquals("alice", sessionStore.session.first()?.username)
        assertTrue(itemDao.items.isEmpty())

        // same account re-login: data preserved
        itemDao.items.add(item(2, unread = true))
        assertEquals(NewsRepository.LoginResult.Success, repo.login("https://example.com", "alice", "token"))
        assertEquals(1, itemDao.items.size)

        // different account: data cleared
        assertEquals(NewsRepository.LoginResult.Success, repo.login("https://example.com", "bob", "token2"))
        assertEquals("bob", sessionStore.session.first()?.username)
        assertTrue(itemDao.items.isEmpty())
    }

    @Test
    fun `login version 0 is rejected without saving session`() = runTest {
        val sessionStore = FakeSessionStore()
        val api = FakeNewsApi(versionResponse = "0")
        val repo = repo(sessionStore = sessionStore, api = api)

        val result = repo.login("https://example.com", "alice", "token")

        assertTrue(result is NewsRepository.LoginResult.Error)
        assertNull(sessionStore.session.first())
    }

    @Test
    fun `sync runs initial when db empty`() = runTest {
        val folderDao = FakeFolderDao()
        val feedDao = FakeFeedDao()
        val itemDao = FakeItemDao()
        val api = FakeNewsApi(
            foldersResponse = FoldersResponse(listOf(FolderDto(1, "Tech"))),
            feedsResponse = FeedsResponse(listOf(FeedDto(10, "http://feed", "Feed"))),
            itemsResponse = ItemsResponse(listOf(ItemDto(100, unread = true))),
        )
        val sessionStore = FakeSessionStore()
        val repo = repo(sessionStore, folderDao, feedDao, itemDao, api)

        repo.login("https://example.com", "alice", "token")
        repo.sync()

        assertEquals(1, folderDao.folders.size)
        assertEquals(1, feedDao.feeds.size)
        assertTrue(itemDao.items.isNotEmpty())
        assertTrue(api.itemsCalls.isNotEmpty()) // initial sync called items()
    }

    @Test
    fun `sync runs incremental when items exist`() = runTest {
        val sessionStore = FakeSessionStore()
        sessionStore.save(Session("https://example.com", "alice", "token"))
        val itemDao = FakeItemDao().apply {
            items.add(item(1, unread = true, lastModified = 700))
        }
        val api = FakeNewsApi(
            itemsUpdatedResponse = ItemsResponse(listOf(ItemDto(2, unread = true, lastModified = 800))),
        )
        val repo = repo(sessionStore, itemDao = itemDao, api = api)

        repo.sync()

        assertEquals(listOf(700L), api.itemsUpdatedLastModified)
        assertEquals(2, itemDao.items.size) // 700 + new 800
    }

    @Test
    fun `setUnread delegates to item dao`() = runTest {
        val itemDao = FakeItemDao()
        val repo = repo(itemDao = itemDao)

        repo.setUnread(42, unread = false)

        assertEquals(listOf(42L to false), itemDao.setUnreadCalls)
    }
}
