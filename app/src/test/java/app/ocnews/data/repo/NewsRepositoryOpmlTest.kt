package app.ocnews.data.repo

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
import org.junit.Test

class NewsRepositoryOpmlTest {

    private suspend fun repo(
        api: FakeNewsApi,
        folderDao: FakeFolderDao = FakeFolderDao(),
    ): NewsRepository {
        val sessionStore = FakeSessionStore()
        sessionStore.save(Session("https://example.com", "alice", "token"))
        return NewsRepository(
            sessionStore = sessionStore,
            folderDao = folderDao,
            feedDao = FakeFeedDao(),
            itemDao = FakeItemDao(),
            apiFactory = { api },
        )
    }

    @Test
    fun `importOpml creates folders once and adds all feeds`() = runTest {
        val api = FakeNewsApi(
            createFolderResponse = FoldersResponse(listOf(FolderDto(100, "Tech"))),
        )
        val r = repo(api)
        val xml = """
            <opml version="2.0"><body>
              <outline text="Tech" title="Tech">
                <outline type="rss" text="A" xmlUrl="https://a.example.com/feed"/>
                <outline type="rss" text="B" xmlUrl="https://b.example.com/feed"/>
              </outline>
              <outline type="rss" text="C" xmlUrl="https://c.example.com/feed"/>
            </body></opml>
        """.trimIndent()

        val result = r.importOpml(xml)

        assertEquals(3, result.feedsAdded)
        assertEquals(1, result.foldersCreated)
        assertEquals(listOf("Tech"), api.createFolderCalls)
        assertEquals(
            listOf(
                "https://a.example.com/feed" to 100L,
                "https://b.example.com/feed" to 100L,
                "https://c.example.com/feed" to null,
            ),
            api.createFeedCalls,
        )
    }

    @Test
    fun `importOpml reuses existing folder`() = runTest {
        val folderDao = FakeFolderDao().apply {
            folders.add(app.ocnews.data.local.FolderEntity(7, "Tech"))
        }
        val api = FakeNewsApi()
        val r = repo(api, folderDao)
        val xml = """
            <opml version="2.0"><body>
              <outline text="Tech" title="Tech">
                <outline type="rss" text="A" xmlUrl="https://a.example.com/feed"/>
              </outline>
            </body></opml>
        """.trimIndent()

        val result = r.importOpml(xml)

        assertEquals(1, result.feedsAdded)
        assertEquals(0, result.foldersCreated) // folder already existed
        assertEquals(emptyList<String>(), api.createFolderCalls)
        assertEquals(listOf("https://a.example.com/feed" to 7L), api.createFeedCalls)
    }
}
