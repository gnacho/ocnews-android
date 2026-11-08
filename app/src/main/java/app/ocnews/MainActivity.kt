package app.ocnews

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ocnews.ui.list.ListViewModel
import app.ocnews.ui.list.MainScreen
import app.ocnews.ui.login.LoginScreen
import app.ocnews.ui.login.LoginViewModel

class MainActivity : ComponentActivity() {

    private val repository get() = (application as OcnewsApplication).container.repository

    private val loginViewModel: LoginViewModel by viewModels {
        LoginViewModel.factory(repository)
    }
    private val listViewModel: ListViewModel by viewModels {
        ListViewModel.factory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val session by repository.session.collectAsStateWithLifecycle(initialValue = null)

            if (session == null) {
                val state by loginViewModel.state.collectAsStateWithLifecycle()
                LoginScreen(
                    state = state,
                    onServerUrlChange = loginViewModel::onServerUrlChange,
                    onUsernameChange = loginViewModel::onUsernameChange,
                    onTokenChange = loginViewModel::onTokenChange,
                    onLogin = loginViewModel::login,
                )
            } else {
                val folders by listViewModel.folders.collectAsStateWithLifecycle()
                val feeds by listViewModel.feeds.collectAsStateWithLifecycle()
                val items by listViewModel.items.collectAsStateWithLifecycle()
                val scope by listViewModel.scopeState.collectAsStateWithLifecycle()
                val onlyUnread by listViewModel.onlyUnreadState.collectAsStateWithLifecycle()
                val syncing by listViewModel.syncing.collectAsStateWithLifecycle()

                MainScreen(
                    folders = folders,
                    feeds = feeds,
                    items = items,
                    scope = scope,
                    onlyUnread = onlyUnread,
                    syncing = syncing,
                    onSelectScope = listViewModel::selectScope,
                    onToggleOnlyUnread = listViewModel::toggleOnlyUnread,
                    onToggleRead = listViewModel::toggleRead,
                    onToggleStarred = listViewModel::toggleStarred,
                    onSync = listViewModel::sync,
                    onLogout = listViewModel::logout,
                )
            }
        }
    }
}
