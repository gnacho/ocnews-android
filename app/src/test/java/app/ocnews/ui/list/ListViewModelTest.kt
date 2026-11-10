package app.ocnews.ui.list

import app.ocnews.data.repo.NetworkStatus
import app.ocnews.data.repo.NewsRepository
import app.ocnews.data.settings.ImageCacheMode
import app.ocnews.data.settings.SearchScope
import app.ocnews.data.session.Session
import app.ocnews.testutil.FakeFeedDao
import app.ocnews.testutil.FakeImagePrefetcher
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
    fun `filterItems keeps articles read this session when only unread`() {
        val items = listOf(
            summary(1, unread = false),
            summary(2, unread = true),
            summary(3, unread = false),
        )
        val result = filterItems(
            items,
            emptyList(),
            Scope.All,
            onlyUnread = true,
            keepReadIds = setOf(1L),
        )
        assertEquals(listOf(1L, 2L), result.map { it.id })
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
    private suspend fun viewModel(
        itemDao: FakeItemDao = FakeItemDao(),
        prefetcher: FakeImagePrefetcher = FakeImagePrefetcher(),
        settingsStore: FakeSettingsStore = FakeSettingsStore(),
        onWifi: Boolean = true,
    ): Pair<ListViewModel, FakeItemDao> {
        val sessionStore = FakeSessionStore()
        sessionStore.save(Session("https://example.com", "alice", "token"))
        val repo = NewsRepository(
            sessionStore = sessionStore,
            folderDao = FakeFolderDao(),
            feedDao = FakeFeedDao(),
            itemDao = itemDao,
            apiFactory = { FakeNewsApi() },
        )
        return ListViewModel(
            repo,
            settingsStore,
            prefetcher,
            NetworkStatus { onWifi },
        ) to itemDao
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
    fun `downloadUnreadArticles stores the full text of every unread article`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val dao = FakeItemDao()
            dao.items.add(item(1, unread = true))
            dao.items.add(item(2, unread = false))
            dao.items.add(item(3, unread = true))
            val (vm, _) = viewModel(itemDao = dao)

            vm.downloadUnreadArticles()
            advanceUntilIdle()

            assertEquals("full body for 1", dao.items.first { it.id == 1L }.fullBody)
            assertEquals(null, dao.items.first { it.id == 2L }.fullBody)
            assertEquals("full body for 3", dao.items.first { it.id == 3L }.fullBody)
            assertEquals(DownloadFinished(DownloadKind.ARTICLES, 2), vm.finished.value)
            assertEquals(null, vm.download.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `downloadUnreadImages prefetches the image of every unread article`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val dao = FakeItemDao()
            dao.items.add(item(1, unread = true).copy(mediaThumbnail = "https://cdn/1.jpg"))
            dao.items.add(item(2, unread = false).copy(mediaThumbnail = "https://cdn/2.jpg"))
            dao.items.add(item(3, unread = true).copy(thumbnailUrl = "https://cdn/3.jpg"))
            val prefetcher = FakeImagePrefetcher()
            val (vm, _) = viewModel(itemDao = dao, prefetcher = prefetcher)

            vm.downloadUnreadImages()
            advanceUntilIdle()

            assertEquals(listOf("https://cdn/1.jpg", "https://cdn/3.jpg"), prefetcher.urls)
            assertEquals(DownloadFinished(DownloadKind.IMAGES, 2), vm.finished.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `downloadUnreadImages is blocked when the policy is never`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val dao = FakeItemDao()
            dao.items.add(item(1, unread = true).copy(mediaThumbnail = "https://cdn/1.jpg"))
            val prefetcher = FakeImagePrefetcher()
            val settings = FakeSettingsStore().apply { setImageCacheMode(ImageCacheMode.NEVER) }
            val (vm, _) = viewModel(itemDao = dao, prefetcher = prefetcher, settingsStore = settings)

            vm.downloadUnreadImages()
            advanceUntilIdle()

            assertEquals(emptyList<String>(), prefetcher.urls)
            assertEquals(DownloadBlocked.DISABLED, vm.finished.value?.blocked)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `downloadUnreadImages is blocked off wifi when the policy is wifi only`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val dao = FakeItemDao()
            dao.items.add(item(1, unread = true).copy(mediaThumbnail = "https://cdn/1.jpg"))
            val prefetcher = FakeImagePrefetcher()
            val settings = FakeSettingsStore().apply { setImageCacheMode(ImageCacheMode.WIFI_ONLY) }
            val (vm, _) = viewModel(
                itemDao = dao,
                prefetcher = prefetcher,
                settingsStore = settings,
                onWifi = false,
            )

            vm.downloadUnreadImages()
            advanceUntilIdle()

            assertEquals(emptyList<String>(), prefetcher.urls)
            assertEquals(DownloadBlocked.WIFI_ONLY, vm.finished.value?.blocked)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `downloadUnreadImages asks off wifi and downloads after confirming`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val dao = FakeItemDao()
            dao.items.add(item(1, unread = true).copy(mediaThumbnail = "https://cdn/1.jpg"))
            val prefetcher = FakeImagePrefetcher()
            val settings = FakeSettingsStore().apply { setImageCacheMode(ImageCacheMode.ASK) }
            val (vm, _) = viewModel(
                itemDao = dao,
                prefetcher = prefetcher,
                settingsStore = settings,
                onWifi = false,
            )

            vm.downloadUnreadImages()
            advanceUntilIdle()
            assertEquals(emptyList<String>(), prefetcher.urls)
            assertTrue(vm.pendingImageDownload.value)

            vm.confirmImageDownload()
            advanceUntilIdle()

            assertEquals(listOf("https://cdn/1.jpg"), prefetcher.urls)
            assertFalse(vm.pendingImageDownload.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `autoMarkRead marks an unread article once`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val (vm, itemDao) = viewModel()

            vm.autoMarkRead(1)
            advanceUntilIdle()
            vm.autoMarkRead(1)
            advanceUntilIdle()

            assertEquals(listOf(1L to false), itemDao.setUnreadCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `autoMarkRead respects an article the user kept unread`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val (vm, itemDao) = viewModel()

            // The reader opened it and it was marked read...
            vm.autoMarkRead(1)
            advanceUntilIdle()
            // ...then the user marks it unread while still reading.
            vm.toggleRead(1, unread = false)
            advanceUntilIdle()
            // Swiping back to it must not mark it read again.
            vm.autoMarkRead(1)
            advanceUntilIdle()

            assertEquals(listOf(1L to false, 1L to true), itemDao.setUnreadCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `loading the detail after auto reading keeps it read`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val dao = FakeItemDao()
            dao.items.add(item(1, unread = true))
            val (vm, _) = viewModel(itemDao = dao)

            // The reader marks it read before its full row is loaded...
            vm.autoMarkRead(1)
            // ...and the row arrives still unread from the database.
            vm.loadItemDetail(1)
            advanceUntilIdle()

            assertEquals(false, vm.itemDetails.value[1]?.unread)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `loading the detail after marking unread keeps it unread`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val dao = FakeItemDao()
            dao.items.add(item(1, unread = false))
            val (vm, _) = viewModel(itemDao = dao)

            vm.toggleRead(1, unread = false) // user marks it unread
            vm.loadItemDetail(1)
            advanceUntilIdle()

            assertEquals(true, vm.itemDetails.value[1]?.unread)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `resetReaderSession lets an article be marked read again`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val (vm, itemDao) = viewModel()

            vm.autoMarkRead(1)
            advanceUntilIdle()
            vm.toggleRead(1, unread = false)
            advanceUntilIdle()
            vm.resetReaderSession()
            vm.autoMarkRead(1)
            advanceUntilIdle()

            assertEquals(listOf(1L to false, 1L to true, 1L to false), itemDao.setUnreadCalls)
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
