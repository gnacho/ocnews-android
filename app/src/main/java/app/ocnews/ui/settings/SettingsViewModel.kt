package app.ocnews.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.ocnews.data.settings.AppSettings
import app.ocnews.data.settings.FeedListStyle
import app.ocnews.data.settings.SearchScope
import app.ocnews.data.settings.SettingsStore
import app.ocnews.data.settings.SortOrder
import app.ocnews.data.settings.SwipeAction
import app.ocnews.data.settings.TextSize
import app.ocnews.data.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settingsStore: SettingsStore) : ViewModel() {
    val settings: StateFlow<AppSettings> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settingsStore.setTheme(mode) }
    }

    fun setSyncInterval(minutes: Int) {
        viewModelScope.launch { settingsStore.setSyncInterval(minutes) }
    }

    fun setSyncOnStart(value: Boolean) {
        viewModelScope.launch { settingsStore.setSyncOnStart(value) }
    }

    fun setOnlyUnread(value: Boolean) {
        viewModelScope.launch { settingsStore.setOnlyUnread(value) }
    }

    fun setSwipeRight(action: SwipeAction) {
        viewModelScope.launch { settingsStore.setSwipeRight(action) }
    }

    fun setSwipeLeft(action: SwipeAction) {
        viewModelScope.launch { settingsStore.setSwipeLeft(action) }
    }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch { settingsStore.setSortOrder(order) }
    }

    fun setSearchIn(scope: SearchScope) {
        viewModelScope.launch { settingsStore.setSearchIn(scope) }
    }

    fun setFeedListStyle(style: FeedListStyle) {
        viewModelScope.launch { settingsStore.setFeedListStyle(style) }
    }

    fun setOledBackground(value: Boolean) {
        viewModelScope.launch { settingsStore.setOledBackground(value) }
    }

    fun setNavigateWithVolumeButtons(value: Boolean) {
        viewModelScope.launch { settingsStore.setNavigateWithVolumeButtons(value) }
    }

    fun setTextSize(size: TextSize) {
        viewModelScope.launch { settingsStore.setTextSize(size) }
    }

    companion object {
        fun factory(settingsStore: SettingsStore): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(settingsStore) as T
            }
    }
}
