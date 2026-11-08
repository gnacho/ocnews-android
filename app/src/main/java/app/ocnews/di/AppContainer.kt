package app.ocnews.di

import android.content.Context
import androidx.room.Room
import app.ocnews.data.local.OcnewsDatabase
import app.ocnews.data.remote.ApiClient
import app.ocnews.data.remote.Credentials
import app.ocnews.data.repo.NewsRepository
import app.ocnews.data.session.DataStoreSessionStore
import app.ocnews.data.session.SessionStore

// Manual DI (no Hilt for the MVP): a single graph owned by the Application.
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: OcnewsDatabase = Room.databaseBuilder(
        appContext,
        OcnewsDatabase::class.java,
        "ocnews.db",
    ).build()

    val sessionStore: SessionStore = DataStoreSessionStore(appContext)

    val repository: NewsRepository = NewsRepository(
        sessionStore = sessionStore,
        folderDao = database.folderDao(),
        feedDao = database.feedDao(),
        itemDao = database.itemDao(),
        apiFactory = { session ->
            ApiClient.create(session.baseUrl) { Credentials(session.username, session.token) }
        },
    )
}
