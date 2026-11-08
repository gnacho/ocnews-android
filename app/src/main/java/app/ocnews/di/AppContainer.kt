package app.ocnews.di

import android.content.Context
import androidx.room.Room
import app.ocnews.data.local.OcnewsDatabase
import app.ocnews.data.remote.ApiClient
import app.ocnews.data.remote.Credentials
import app.ocnews.data.repo.NewsRepository
import app.ocnews.data.session.DataStoreSessionStore
import app.ocnews.data.session.SessionStore
import app.ocnews.data.settings.DataStoreSettingsStore
import app.ocnews.data.settings.SettingsStore

// Manual DI (no Hilt for the MVP): a single graph owned by the Application.
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: OcnewsDatabase = Room.databaseBuilder(
        appContext,
        OcnewsDatabase::class.java,
        "ocnews.db",
    ).addMigrations(OcnewsDatabase.MIGRATION_1_2).build()

    val sessionStore: SessionStore = DataStoreSessionStore(appContext)

    val settingsStore: SettingsStore = DataStoreSettingsStore(appContext)

    val repository: NewsRepository = NewsRepository(
        sessionStore = sessionStore,
        folderDao = database.folderDao(),
        feedDao = database.feedDao(),
        itemDao = database.itemDao(),
        apiFactory = { session ->
            ApiClient.create(session.baseUrl) { Credentials(session.username, session.token) }
        },
    )

    // Moves a legacy plaintext token into the encrypted store (one-time).
    suspend fun migrateLegacyToken() {
        (sessionStore as? DataStoreSessionStore)?.migrateLegacyToken()
    }
}
