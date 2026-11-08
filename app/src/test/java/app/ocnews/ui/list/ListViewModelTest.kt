package app.ocnews.ui.list

import app.ocnews.data.local.ItemEntity
import app.ocnews.data.repo.NewsRepository
import app.ocnews.testutil.FakeFeedDao
import app.ocnews.testutil.FakeFolderDao
import app.ocnews.testutil.FakeItemDao
import app.ocnews.testutil.FakeNewsApi
import app.ocnews.testutil.FakeSessionStore
import app.ocnews.testutil.feed
import app.ocnews.testutil.item
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
            item(1, unread = true),
            item(2, unread = false, starred = true),
        )
        assertEquals(items, filterItems(items, emptyList(), Scope.All, onlyUnread = false))
    }

    @Test
    fun `filterItems starred only keeps starred`() {
        val items = listOf(
            item(1, unread = true),
            item(2, unread = false, starred = true),
        )
        val result = filterItems(items, emptyList(), Scope.Starred, onlyUnread = false)
        assertEquals(listOf(2L), result.map { it.id })
    }

    @Test
    fun `filterItems feed only keeps matching feed`() {
        val items = listOf(
            item(1, unread = true).copy(feedId = 10),
            item(2, unread = true).copy(feedId = 20),
        )
        val result = filterItems(items, emptyList(), Scope.Feed(10), onlyUnread = false)
        assertEquals(listOf(1L), result.map { it.id })
    }

    @Test
    fun `filterItems folder keeps feeds in that folder`() {
        val items = listOf(
            item(1, unread = true).copy(feedId = 10),
            item(2, unread = true).copy(feedId = 20),
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
            item(1, unread = true),
            item(2, unread = false),
        )
        val result = filterItems(items, emptyList(), Scope.All, onlyUnread = true)
        assertEquals(listOf(1L), result.map { it.id })
    }

    @Test
    fun `filterItems query matches title and author case-insensitively`() {
        val items = listOf(
            item(1, unread = true).copy(title = "Butterflies", author = "Alice"),
            item(2, unread = true).copy(title = "Other", author = "Bob"),
            item(3, unread = true).copy(title = "Unrelated", author = "Alice Smith"),
        )
        val result = filterItems(items, emptyList(), Scope.All, onlyUnread = false, query = "alice")
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
        return ListViewModel(repo) to itemDao
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `toggleRead flips unread on the item`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val (vm, itemDao) = viewModel()
            val unreadItem: ItemEntity = item(1, unread = true)

            vm.toggleRead(unreadItem)
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
            val item: ItemEntity = item(1, unread = false, starred = false)

            vm.toggleStarred(item)
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
