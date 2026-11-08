package app.ocnews.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.ocnews.data.local.FeedEntity
import app.ocnews.data.local.FolderEntity
import app.ocnews.data.local.ItemEntity
import app.ocnews.data.repo.NewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface Scope {
    data object All : Scope
    data object Starred : Scope
    data class Feed(val id: Long) : Scope
    data class Folder(val id: Long) : Scope
}

// Pure filter, unit-testable without coroutines.
fun filterItems(
    items: List<ItemEntity>,
    feeds: List<FeedEntity>,
    scope: Scope,
    onlyUnread: Boolean,
): List<ItemEntity> {
    val scoped = when (scope) {
        Scope.All -> items
        Scope.Starred -> items.filter { it.starred }
        is Scope.Feed -> items.filter { it.feedId == scope.id }
        is Scope.Folder -> {
            val feedIds = feeds.filter { it.folderId == scope.id }.map { it.id }.toSet()
            items.filter { it.feedId in feedIds }
        }
    }
    return if (onlyUnread) scoped.filter { it.unread } else scoped
}

class ListViewModel(private val repository: NewsRepository) : ViewModel() {
    private val scope = MutableStateFlow<Scope>(Scope.All)
    private val onlyUnread = MutableStateFlow(false)
    private val _syncing = MutableStateFlow(false)

    init {
        // Sync on open: initial when empty, incremental otherwise.
        sync()
    }

    val scopeState: StateFlow<Scope> = scope
    val onlyUnreadState: StateFlow<Boolean> = onlyUnread
    val syncing: StateFlow<Boolean> = _syncing

    val folders: StateFlow<List<FolderEntity>> = repository.observeFolders()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val feeds: StateFlow<List<FeedEntity>> = repository.observeFeeds()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val unreadCount: StateFlow<Int> = repository.observeUnread()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val starredCount: StateFlow<Int> = repository.observeStarred()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val items: StateFlow<List<ItemEntity>> =
        combine(scope, onlyUnread, repository.observeFeeds()) { s, u, feeds -> Triple(s, u, feeds) }
            .flatMapLatest { (s, u, feeds) ->
                when (s) {
                    Scope.All -> repository.observeItems()
                    Scope.Starred -> repository.observeStarred()
                    is Scope.Feed -> repository.observeItemsByFeed(s.id)
                    is Scope.Folder -> repository.observeItems()
                }.map { list -> filterItems(list, feeds, s, u) }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun selectScope(s: Scope) {
        scope.value = s
    }

    fun toggleOnlyUnread() {
        onlyUnread.value = !onlyUnread.value
    }

    fun toggleRead(item: ItemEntity) {
        viewModelScope.launch { repository.setUnread(item.id, !item.unread) }
    }

    fun toggleStarred(item: ItemEntity) {
        viewModelScope.launch { repository.setStarred(item.id, !item.starred) }
    }

    fun sync() {
        viewModelScope.launch {
            _syncing.value = true
            try {
                repository.sync()
            } finally {
                _syncing.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch { repository.logout() }
    }

    companion object {
        fun factory(repository: NewsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ListViewModel(repository) as T
            }
    }
}
