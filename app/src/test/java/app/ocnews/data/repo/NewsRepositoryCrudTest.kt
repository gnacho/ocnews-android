package app.ocnews.data.repo

import app.ocnews.data.remote.dto.DiscoveredFeedDto
import app.ocnews.data.remote.dto.DiscoverResponse
import app.ocnews.data.remote.dto.FeedDto
import app.ocnews.data.remote.dto.FeedsResponse
import app.ocnews.data.remote.dto.FolderDto
import app.ocnews.data.remote.dto.FoldersResponse
import app.ocnews.data.session.Session
import app.ocnews.testutil.FakeFeedDao
import app.ocnews.testutil.FakeFolderDao
import app.ocnews.testutil.FakeItemDao
import app.ocnews.testutil.FakeNewsApi
import app.ocnews.testutil.FakeSessionStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsRepositoryCrudTest {

    private suspend fun loggedInRepo(
        api: FakeNewsApi = FakeNewsApi(),
        folderDao: FakeFolderDao = FakeFolderDao(),
        feedDao: FakeFeedDao = FakeFeedDao(),
    ): Pair<NewsRepository, FakeSessionStore> {
        val sessionStore = FakeSessionStore()
        sessionStore.save(Session("https://example.com", "alice", "token"))
        val repo = NewsRepository(
            sessionStore = sessionStore,
            folderDao = folderDao,
            feedDao = feedDao,
            itemDao = FakeItemDao(),
            apiFactory = { api },
        )
        return repo to sessionStore
    }

    @Test
    fun `addFeed posts and upserts returned feed`() = runTest {
        val feedDao = FakeFeedDao()
        val api = FakeNewsApi(
            createFeedResponse = FeedsResponse(listOf(FeedDto(10, "http://feed", "Feed"))),
        )
        val (repo) = loggedInRepo(api, feedDao = feedDao)

        repo.addFeed("http://feed", folderId = 3)

        assertEquals(listOf("http://feed" to 3L), api.createFeedCalls)
        assertEquals(1, feedDao.feeds.size)
        assertEquals(10L, feedDao.feeds.first().id)
    }

    @Test
    fun `createFolder posts and upserts returned folder`() = runTest {
        val folderDao = FakeFolderDao()
        val api = FakeNewsApi(
            createFolderResponse = FoldersResponse(listOf(FolderDto(7, "Tech"))),
        )
        val (repo) = loggedInRepo(api, folderDao = folderDao)

        repo.createFolder("Tech")

        assertEquals(listOf("Tech"), api.createFolderCalls)
        assertEquals(1, folderDao.folders.size)
        assertEquals(7L, folderDao.folders.first().id)
    }

    @Test
    fun `deleteFeed calls api and dao`() = runTest {
        val feedDao = FakeFeedDao()
        val api = FakeNewsApi()
        val (repo) = loggedInRepo(api, feedDao = feedDao)

        repo.deleteFeed(42)

        assertEquals(listOf(42L), api.deleteFeedCalls)
    }

    @Test
    fun `moveFeed to root sends folderId 0`() = runTest {
        val api = FakeNewsApi()
        val (repo) = loggedInRepo(api)

        repo.moveFeed(5, folderId = null)

        assertEquals(listOf(5L to 0L), api.moveFeedCalls)
    }

    @Test
    fun `renameFeed calls api and dao`() = runTest {
        val feedDao = FakeFeedDao()
        val api = FakeNewsApi()
        val (repo) = loggedInRepo(api, feedDao = feedDao)

        repo.renameFeed(5, "New title")

        assertEquals(listOf(5L to "New title"), api.renameFeedCalls)
    }

    @Test
    fun `discover returns feeds`() = runTest {
        val api = FakeNewsApi(
            discoverResponse = DiscoverResponse(
                listOf(DiscoveredFeedDto("http://feed", "Feed", "rss"))
            )
        )
        val (repo) = loggedInRepo(api)

        val result = repo.discover("http://site")

        assertEquals(1, result.size)
        assertEquals("http://feed", result.first().url)
    }

    @Test
    fun `markAllRead calls api`() = runTest {
        val api = FakeNewsApi()
        val (repo) = loggedInRepo(api)

        repo.markAllRead()

        assertEquals(1, api.markAllReadCalls)
    }

    @Test
    fun `operations are no-op without a session`() = runTest {
        val api = FakeNewsApi()
        val repo = NewsRepository(
            sessionStore = FakeSessionStore(), // no session
            folderDao = FakeFolderDao(),
            feedDao = FakeFeedDao(),
            itemDao = FakeItemDao(),
            apiFactory = { api },
        )

        repo.addFeed("http://feed")
        repo.deleteFeed(1)
        repo.markAllRead()

        assertTrue(api.createFeedCalls.isEmpty())
        assertTrue(api.deleteFeedCalls.isEmpty())
        assertEquals(0, api.markAllReadCalls)
    }
}
