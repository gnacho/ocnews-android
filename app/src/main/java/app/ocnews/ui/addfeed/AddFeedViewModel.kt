package app.ocnews.ui.addfeed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.ocnews.R
import app.ocnews.data.remote.dto.DiscoveredFeedDto
import app.ocnews.data.repo.NewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class AddFeedUiState(
    val url: String = "",
    val loading: Boolean = false,
    val errorRes: Int? = null,
    val errorArgs: List<Any> = emptyList(),
    val discovered: List<DiscoveredFeedDto> = emptyList(),
    val done: Boolean = false,
)

class AddFeedViewModel(private val repository: NewsRepository) : ViewModel() {
    private val _state = MutableStateFlow(AddFeedUiState())
    val state: StateFlow<AddFeedUiState> = _state

    // Selected destination folder (null = root). Set by the UI before add().
    var folderId: Long? = null

    fun onUrlChange(value: String) =
        _state.update { it.copy(url = value, errorRes = null, errorArgs = emptyList(), discovered = emptyList()) }

    fun add() {
        val url = _state.value.url.trim()
        if (url.isEmpty()) {
            _state.update { it.copy(errorRes = R.string.err_enter_url, errorArgs = emptyList()) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorRes = null, errorArgs = emptyList(), discovered = emptyList()) }
            try {
                repository.addFeed(url, folderId)
                _state.update { it.copy(loading = false, done = true) }
            } catch (e: HttpException) {
                if (e.code() == 422) {
                    val discovered = repository.discover(url)
                    if (discovered.isEmpty()) {
                        _state.update { it.copy(loading = false, errorRes = R.string.err_no_feeds, errorArgs = emptyList()) }
                    } else {
                        _state.update { it.copy(loading = false, discovered = discovered) }
                    }
                } else {
                    _state.update { it.copy(loading = false, errorRes = R.string.err_server, errorArgs = listOf(e.code())) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, errorRes = R.string.err_generic, errorArgs = emptyList()) }
            }
        }
    }

    fun selectDiscovered(feed: DiscoveredFeedDto) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, discovered = emptyList()) }
            try {
                repository.addFeed(feed.url, folderId)
                _state.update { it.copy(loading = false, done = true) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, errorRes = R.string.err_generic, errorArgs = emptyList()) }
            }
        }
    }

    companion object {
        fun factory(repository: NewsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AddFeedViewModel(repository) as T
            }
    }
}
