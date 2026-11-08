package app.ocnews.ui.settings

import app.ocnews.data.settings.AppSettings
import app.ocnews.data.settings.ThemeMode
import app.ocnews.testutil.FakeSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @Test
    fun `defaults to system theme and 15 min`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val store = FakeSettingsStore()
            val vm = SettingsViewModel(store)
            advanceUntilIdle()
            assertEquals(AppSettings(ThemeMode.SYSTEM, 15), vm.settings.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `setTheme persists dark mode`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val store = FakeSettingsStore()
            val vm = SettingsViewModel(store)
            vm.setTheme(ThemeMode.DARK)
            advanceUntilIdle()
            assertEquals(ThemeMode.DARK, vm.settings.value.theme)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `setSyncInterval persists the value`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val store = FakeSettingsStore()
            val vm = SettingsViewModel(store)
            vm.setSyncInterval(60)
            advanceUntilIdle()
            assertEquals(60, vm.settings.value.syncIntervalMinutes)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
