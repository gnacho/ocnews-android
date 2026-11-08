package app.ocnews

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.ocnews.ui.addfeed.AddFeedViewModel
import app.ocnews.ui.list.ListViewModel
import app.ocnews.ui.list.MainScreen
import app.ocnews.ui.login.LoginScreen
import app.ocnews.ui.login.LoginViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val repository get() = (application as OcnewsApplication).container.repository

    private val loginViewModel: LoginViewModel by viewModels {
        LoginViewModel.factory(repository)
    }
    private val listViewModel: ListViewModel by viewModels {
        ListViewModel.factory(repository)
    }
    private val addFeedViewModel: AddFeedViewModel by viewModels {
        AddFeedViewModel.factory(repository)
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

                val context = LocalContext.current
                val opmlLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) {
                        lifecycleScope.launch {
                            val xml = context.contentResolver
                                .openInputStream(uri)
                                ?.bufferedReader()
                                ?.use { it.readText() }
                            if (xml != null) {
                                val result = repository.importOpml(xml)
                                Toast.makeText(
                                    context,
                                    "Imported ${result.feedsAdded} feeds" +
                                        if (result.foldersCreated > 0) " (${result.foldersCreated} folders)" else "",
                                    Toast.LENGTH_LONG,
                                ).show()
                            } else {
                                Toast.makeText(context, "Could not read file", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                MainScreen(
                    folders = folders,
                    feeds = feeds,
                    items = items,
                    scope = scope,
                    onlyUnread = onlyUnread,
                    syncing = syncing,
                    addFeedViewModel = addFeedViewModel,
                    onSelectScope = listViewModel::selectScope,
                    onToggleOnlyUnread = listViewModel::toggleOnlyUnread,
                    onToggleRead = listViewModel::toggleRead,
                    onToggleStarred = listViewModel::toggleStarred,
                    onSync = listViewModel::sync,
                    onLogout = listViewModel::logout,
                    onImportOpml = { opmlLauncher.launch(arrayOf("*/*")) },
                )
            }
        }
    }
}
