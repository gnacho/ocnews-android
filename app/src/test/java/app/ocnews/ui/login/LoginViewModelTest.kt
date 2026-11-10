package app.ocnews.ui.login

import app.ocnews.data.repo.NewsRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private fun viewModel(api: FakeNewsApi = FakeNewsApi()): LoginViewModel {
        val repo = NewsRepository(
            sessionStore = FakeSessionStore(),
            folderDao = FakeFolderDao(),
            feedDao = FakeFeedDao(),
            itemDao = FakeItemDao(),
            apiFactory = { api },
        )
        return LoginViewModel(
            repo,
            serverProbe = { app.ocnews.data.remote.ServerProbe.Result.Ok("34.0.0") },
        )
    }

    @Test
    fun `login success sets loggedIn`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val vm = viewModel()
            vm.onServerUrlChange("https://example.com")
            vm.onUsernameChange("alice")
            vm.onTokenChange("token")

            vm.login()
            advanceUntilIdle()

            assertTrue(vm.state.value.loggedIn)
            assertFalse(vm.state.value.loading)
            assertEquals(null, vm.state.value.errorRes)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `login failure sets error`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val vm = viewModel(FakeNewsApi(versionResponse = "0"))
            vm.onServerUrlChange("https://example.com")
            vm.onUsernameChange("alice")
            vm.onTokenChange("token")

            vm.login()
            advanceUntilIdle()

            assertFalse(vm.state.value.loggedIn)
            assertTrue(vm.state.value.errorRes != null)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
