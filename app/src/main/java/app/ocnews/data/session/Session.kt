package app.ocnews.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Session(
    val serverUrl: String,
    val username: String,
    val token: String,
) {
    val baseUrl: String
        get() = serverUrl.trimEnd('/') + "/index.php/apps/news/api/v1-3/"
}

interface SessionStore {
    val session: Flow<Session?>
    suspend fun save(session: Session)
    suspend fun clear()
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

class DataStoreSessionStore(private val context: Context) : SessionStore {
    private val keyUrl = stringPreferencesKey("server_url")
    private val keyUser = stringPreferencesKey("username")
    private val keyToken = stringPreferencesKey("token")

    override val session: Flow<Session?> = context.dataStore.data.map { prefs ->
        val url = prefs[keyUrl] ?: return@map null
        val user = prefs[keyUser] ?: return@map null
        val token = prefs[keyToken] ?: return@map null
        Session(url, user, token)
    }

    override suspend fun save(session: Session) {
        context.dataStore.edit { prefs ->
            prefs[keyUrl] = session.serverUrl
            prefs[keyUser] = session.username
            prefs[keyToken] = session.token
        }
    }

    override suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
