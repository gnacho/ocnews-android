package app.ocnews.data.repo

import app.ocnews.data.local.FeedDao
import app.ocnews.data.local.FeedEntity
import app.ocnews.data.local.FolderDao
import app.ocnews.data.local.FolderEntity
import app.ocnews.data.local.ItemDao
import app.ocnews.data.local.ItemEntity
import app.ocnews.data.remote.NewsApi
import app.ocnews.data.session.Session
import app.ocnews.data.session.SessionStore
import app.ocnews.sync.SyncEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.HttpException

// Single concurrency boundary between UI (ViewModels) and data sources.
class NewsRepository(
    private val sessionStore: SessionStore,
    private val folderDao: FolderDao,
    private val feedDao: FeedDao,
    private val itemDao: ItemDao,
    private val apiFactory: (Session) -> NewsApi,
) {
    val session: Flow<Session?> = sessionStore.session

    sealed interface LoginResult {
        data object Success : LoginResult
        data class Error(val message: String) : LoginResult
    }

    suspend fun login(serverUrl: String, username: String, token: String): LoginResult {
        val newSession = Session(serverUrl, username, token)
        return try {
            val version = apiFactory(newSession).version()
            if (version.version == "0") {
                LoginResult.Error("News app reports version 0")
            } else {
                val previous = sessionStore.session.first()
                val changedAccount =
                    previous?.username != username || previous?.serverUrl != serverUrl
                sessionStore.save(newSession)
                if (changedAccount) clearLocalData()
                LoginResult.Success
            }
        } catch (e: HttpException) {
            val msg = if (e.code() == 401) {
                "Invalid username or app token"
            } else {
                "Server error (${e.code()})"
            }
            LoginResult.Error(msg)
        } catch (e: Exception) {
            LoginResult.Error(e.message ?: "Login failed")
        }
    }

    suspend fun logout() {
        sessionStore.clear()
        clearLocalData()
    }

    suspend fun sync() {
        val session = sessionStore.session.first() ?: return
        val engine = SyncEngine(apiFactory(session), folderDao, feedDao, itemDao)
        engine.pushPendingState()
        val hasItems = itemDao.getHighestLastModified() != null
        if (hasItems) engine.incrementalSync() else engine.initialSync()
    }

    suspend fun setUnread(itemId: Long, unread: Boolean) = itemDao.setUnread(itemId, unread)

    suspend fun setStarred(itemId: Long, starred: Boolean) = itemDao.setStarred(itemId, starred)

    fun observeFolders(): Flow<List<FolderEntity>> = folderDao.getAll()
    fun observeFeeds(): Flow<List<FeedEntity>> = feedDao.getAll()
    fun observeItems(): Flow<List<ItemEntity>> = itemDao.getAll()
    fun observeUnread(): Flow<List<ItemEntity>> = itemDao.getUnread()
    fun observeStarred(): Flow<List<ItemEntity>> = itemDao.getStarred()

    private suspend fun clearLocalData() {
        folderDao.clear()
        feedDao.clear()
        itemDao.clear()
    }
}
