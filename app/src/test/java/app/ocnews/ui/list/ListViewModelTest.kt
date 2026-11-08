package app.ocnews.ui.list

import app.ocnews.data.repo.NewsRepository
import app.ocnews.data.settings.SearchScope
import app.ocnews.testutil.FakeFeedDao
import app.ocnews.testutil.FakeFolderDao
import app.ocnews.testutil.FakeItemDao
import app.ocnews.testutil.FakeNewsApi
import app.ocnews.testutil.FakeSessionStore
import app.ocnews.testutil.FakeSettingsStore
import app.ocnews.testutil.feed
import app.ocnews.testutil.item
import app.ocnews.testutil.summary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListViewModelTest {

    // --- filterItems (pure) ---

    @Test
    fun `filterItems all includes everything`() {
        val items = listOf(
            summary(1, unread = true),
            summary(2, unread = false, starred = true),
        )
        assertEquals(items, filterItems(items, emptyList(), Scope.All, onlyUnread = false))
    }

    @Test
    fun `filterItems starred only keeps starred`() {
        val items = listOf(
            summary(1, unread = true),
            summary(2, unread = false, starred = true),
        )
        val result = filterItems(items, emptyList(), Scope.Starred, onlyUnread = false)
        assertEquals(listOf(2L), result.map { it.id })
    }

    @Test
    fun `filterItems feed only keeps matching feed`() {
        val items = listOf(
            summary(1, unread = true, feedId = 10),
            summary(2, unread = true, feedId = 20),
        )
        val result = filterItems(items, emptyList(), Scope.Feed(10), onlyUnread = false)
        assertEquals(listOf(1L), result.map { it.id })
    }

    @Test
    fun `filterItems folder keeps feeds in that folder`() {
        val items = listOf(
            summary(1, unread = true, feedId = 10),
            summary(2, unread = true, feedId = 20),
        )
        val feeds = listOf(
            feed(10, folderId = 100),
            feed(20, folderId = 200),
        )
        val result = filterItems(items, feeds, Scope.Folder(100), onlyUnread = false)
        assertEquals(listOf(1L), result.map { it.id })
    }

    @Test
    fun `filterItems onlyUnread filters read items`() {
        val items = listOf(
            summary(1, unread = true),
            summary(2, unread = false),
        )
        val result = filterItems(items, emptyList(), Scope.All, onlyUnread = true)
        assertEquals(listOf(1L), result.map { it.id })
    }

    @Test
    fun `filterItems query matches title case-insensitively`() {
        val items = listOf(
            summary(1, unread = true, title = "Butterflies"),
            summary(2, unread = true, title = "Other"),
            summary(3, unread = true, title = "Butterfly effect"),
        )
        val result = filterItems(items, emptyList(), Scope.All, onlyUnread = false, query = "butterfl")
        assertEquals(listOf(1L, 3L), result.map { it.id })
    }

    @Test
    fun `filterItems content search uses the ids resolved by SQL`() {
        val items = listOf(
            summary(1, unread = true, title = "Other"),
            summary(2, unread = true, title = "Other"),
        )
        val result = filterItems(
            items,
            emptyList(),
            Scope.All,
            onlyUnread = false,
            query = "butterfl",
            searchIn = SearchScope.CONTENT,
            contentMatchIds = setOf(2L),
        )
        assertEquals(listOf(2L), result.map { it.id })
    }

    @Test
    fun `filterItems both search matches title or body`() {
        val items = listOf(
            summary(1, unread = true, title = "Butterflies"),
            summary(2, unread = true, title = "Other"),
            summary(3, unread = true, title = "Other"),
        )
        val result = filterItems(
            items,
            emptyList(),
            Scope.All,
            onlyUnread = false,
            query = "butterfl",
            searchIn = SearchScope.BOTH,
            contentMatchIds = setOf(3L),
        )
        assertEquals(listOf(1L, 3L), result.map { it.id })
    }

    // --- ViewModel ---

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun viewModel(itemDao: FakeItemDao = FakeItemDao()): Pair<ListViewModel, FakeItemDao> {
        val repo = NewsRepository(
            sessionStore = FakeSessionStore(),
            folderDao = FakeFolderDao(),
            feedDao = FakeFeedDao(),
            itemDao = itemDao,
            apiFactory = { FakeNewsApi() },
        )
        return ListViewModel(repo, FakeSettingsStore()) to itemDao
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `toggleRead flips unread on the item`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val (vm, itemDao) = viewModel()

            vm.toggleRead(1L, unread = true)
            advanceUntilIdle()

            assertEquals(listOf(1L to false), itemDao.setUnreadCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `toggleStarred flips starred on the item`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val (vm, itemDao) = viewModel()

            vm.toggleStarred(1L, starred = false)
            advanceUntilIdle()

            assertEquals(listOf(1L to true), itemDao.setStarredCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `selectScope updates scope state`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val (vm) = viewModel()
            assertEquals(Scope.All, vm.scopeState.value)

            vm.selectScope(Scope.Starred)
            advanceUntilIdle()

            assertEquals(Scope.Starred, vm.scopeState.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `toggleOnlyUnread flips the flag`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val (vm) = viewModel()
            assertFalse(vm.onlyUnreadState.value)

            vm.toggleOnlyUnread()
            advanceUntilIdle()

            assertTrue(vm.onlyUnreadState.value)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
