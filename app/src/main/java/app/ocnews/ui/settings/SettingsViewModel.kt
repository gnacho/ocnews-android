package app.ocnews.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.ocnews.data.settings.AppSettings
import app.ocnews.data.settings.SettingsStore
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

    companion object {
        fun factory(settingsStore: SettingsStore): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(settingsStore) as T
            }
    }
}
