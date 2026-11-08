package app.ocnews.ui.addfeed

import app.ocnews.R
import app.ocnews.data.remote.dto.DiscoveredFeedDto
import app.ocnews.data.remote.dto.DiscoverResponse
import app.ocnews.data.repo.NewsRepository
import app.ocnews.data.session.Session
import app.ocnews.testutil.FakeFeedDao
import app.ocnews.testutil.FakeFolderDao
import app.ocnews.testutil.FakeItemDao
import app.ocnews.testutil.FakeNewsApi
import app.ocnews.testutil.FakeSessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddFeedViewModelTest {

    private suspend fun viewModel(api: FakeNewsApi = FakeNewsApi()): AddFeedViewModel {
        val sessionStore = FakeSessionStore()
        sessionStore.save(Session("https://example.com", "alice", "token"))
        val repo = NewsRepository(
            sessionStore = sessionStore,
            folderDao = FakeFolderDao(),
            feedDao = FakeFeedDao(),
            itemDao = FakeItemDao(),
            apiFactory = { api },
        )
        return AddFeedViewModel(repo)
    }

    @Test
    fun `empty url shows error without calling api`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val api = FakeNewsApi()
            val vm = viewModel(api)
            vm.add()
            advanceUntilIdle()
            assertEquals(R.string.err_enter_url, vm.state.value.errorRes)
            assertTrue(api.createFeedCalls.isEmpty())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `successful add sets done`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val api = FakeNewsApi()
            val vm = viewModel(api)
            vm.folderId = 3
            vm.onUrlChange("http://example.com/feed")
            vm.add()
            advanceUntilIdle()

            assertTrue(vm.state.value.done)
            assertEquals(listOf("http://example.com/feed" to 3L), api.createFeedCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `422 falls back to discover and shows candidates`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val api = FakeNewsApi(
                createFeedErrorCode = 422,
                discoverResponse = DiscoverResponse(
                    listOf(DiscoveredFeedDto("http://example.com/rss", "Blog", "rss"))
                ),
            )
            val vm = viewModel(api)
            vm.onUrlChange("http://example.com")
            vm.add()
            advanceUntilIdle()

            assertEquals(1, vm.state.value.discovered.size)
            assertEquals("http://example.com/rss", vm.state.value.discovered.first().url)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `422 with no discover result shows error`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val api = FakeNewsApi(createFeedErrorCode = 422, discoverResponse = DiscoverResponse(emptyList()))
            val vm = viewModel(api)
            vm.onUrlChange("http://example.com")
            vm.add()
            advanceUntilIdle()

            assertTrue(vm.state.value.discovered.isEmpty())
            assertEquals(R.string.err_no_feeds, vm.state.value.errorRes)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `selectDiscovered adds the chosen feed`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val api = FakeNewsApi()
            val vm = viewModel(api)
            vm.folderId = null
            vm.selectDiscovered(DiscoveredFeedDto("http://example.com/rss", "Blog", "rss"))
            advanceUntilIdle()

            assertTrue(vm.state.value.done)
            assertEquals(listOf("http://example.com/rss" to null), api.createFeedCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
