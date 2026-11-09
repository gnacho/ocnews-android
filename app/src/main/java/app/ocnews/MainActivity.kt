package app.ocnews

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.ocnews.data.settings.AppSettings
import app.ocnews.data.settings.TextSize
import app.ocnews.data.settings.ThemeMode
import app.ocnews.sync.SyncScheduler
import app.ocnews.ui.addfeed.AddFeedViewModel
import app.ocnews.ui.list.ListViewModel
import app.ocnews.ui.list.MainScreen
import app.ocnews.ui.login.LoginScreen
import app.ocnews.ui.login.LoginViewModel
import app.ocnews.ui.settings.SettingsScreen
import app.ocnews.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val repository get() = (application as OcnewsApplication).container.repository
    private val settingsStore get() = (application as OcnewsApplication).container.settingsStore
    private val imagePrefetcher get() = (application as OcnewsApplication).container.imagePrefetcher
    private val networkStatus get() = (application as OcnewsApplication).container.networkStatus

    private val loginViewModel: LoginViewModel by viewModels {
        LoginViewModel.factory(repository)
    }
    private val listViewModel: ListViewModel by viewModels {
        ListViewModel.factory(repository, settingsStore, imagePrefetcher, networkStatus)
    }
    private val addFeedViewModel: AddFeedViewModel by viewModels {
        AddFeedViewModel.factory(repository)
    }
    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModel.factory(settingsStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            val darkTheme = when (settings.theme) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            val fontScale = when (settings.textSize) {
                TextSize.SMALL -> 0.88f
                TextSize.DEFAULT -> 1f
                TextSize.LARGE -> 1.15f
            }
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, density.fontScale * fontScale),
            ) {
                val colorScheme = if (darkTheme) {
                    if (settings.oledBackground) {
                        darkColorScheme(
                            background = Color.Black,
                            surface = Color.Black,
                            surfaceContainer = Color(0xFF0D0D0D),
                            surfaceContainerLow = Color(0xFF0A0A0A),
                            surfaceContainerHigh = Color(0xFF141414),
                            surfaceContainerHighest = Color(0xFF1A1A1A),
                        )
                    } else {
                        darkColorScheme()
                    }
                } else {
                    lightColorScheme()
                }
                MaterialTheme(colorScheme = colorScheme) {
                val session by repository.session.collectAsStateWithLifecycle(initialValue = null)
                val currentSession = session

                if (currentSession == null) {
                    val state by loginViewModel.state.collectAsStateWithLifecycle()
                    LoginScreen(
                        state = state,
                        onServerUrlChange = loginViewModel::onServerUrlChange,
                        onUsernameChange = loginViewModel::onUsernameChange,
                        onTokenChange = loginViewModel::onTokenChange,
                        onLogin = loginViewModel::login,
                    )
                } else {
                    var showSettings by remember { mutableStateOf(false) }
                    LaunchedEffect(settings.syncIntervalMinutes) {
                        SyncScheduler.schedule(this@MainActivity, settings.syncIntervalMinutes.toLong())
                    }

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

                    if (showSettings) {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onImportOpml = { opmlLauncher.launch(arrayOf("*/*")) },
                            onCreateFolder = { name ->
                                lifecycleScope.launch {
                                    runCatching { repository.createFolder(name) }
                                        .onFailure { Toast.makeText(context, it.message ?: "Error", Toast.LENGTH_SHORT).show() }
                                }
                            },
                            addFeedViewModel = addFeedViewModel,
                            onClearCache = {
                                lifecycleScope.launch {
                                    runCatching { repository.clearOfflineCache() }
                                    runCatching {
                                        coil3.SingletonImageLoader.get(applicationContext).diskCache?.clear()
                                    }
                                    Toast.makeText(context, R.string.clear_cache_done, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onBack = { showSettings = false },
                        )
                    } else {
                        val folders by listViewModel.folders.collectAsStateWithLifecycle()
                        val feeds by listViewModel.feeds.collectAsStateWithLifecycle()
                        val items by listViewModel.items.collectAsStateWithLifecycle()
                        val scope by listViewModel.scopeState.collectAsStateWithLifecycle()
                        val onlyUnread by listViewModel.onlyUnreadState.collectAsStateWithLifecycle()
                        val syncing by listViewModel.syncing.collectAsStateWithLifecycle()
                        val unreadCount by listViewModel.unreadCount.collectAsStateWithLifecycle()
                        val starredCount by listViewModel.starredCount.collectAsStateWithLifecycle()
                        val searchQuery by listViewModel.searchQuery.collectAsStateWithLifecycle()
                        val fullContent by listViewModel.fullContent.collectAsStateWithLifecycle()
                        val itemDetails by listViewModel.itemDetails.collectAsStateWithLifecycle()
                        val download by listViewModel.download.collectAsStateWithLifecycle()
                        val finished by listViewModel.finished.collectAsStateWithLifecycle()
                        val pendingImageDownload by
                            listViewModel.pendingImageDownload.collectAsStateWithLifecycle()
                        val fullTextFeedIds by listViewModel.fullTextFeedIds.collectAsStateWithLifecycle()

                        MainScreen(
                            folders = folders,
                            feeds = feeds,
                            items = items,
                            scope = scope,
                            onlyUnread = onlyUnread,
                            syncing = syncing,
                            unreadCount = unreadCount,
                            starredCount = starredCount,
                            serverUrl = currentSession.serverUrl,
                            settings = settings,
                            isDark = darkTheme,
                            searchQuery = searchQuery,
                            onSearchQueryChange = listViewModel::setSearchQuery,
                            fullContent = fullContent,
                            itemDetails = itemDetails,
                            navigateWithVolume = settings.navigateWithVolumeButtons,
                            hideRelated = settings.hideRelatedSections,
                            download = download,
                            finished = finished,
                            pendingImageDownload = pendingImageDownload,
                            onConfirmImageDownload = listViewModel::confirmImageDownload,
                            onCancelImageDownload = listViewModel::cancelImageDownload,
                            onDownloadArticles = listViewModel::downloadUnreadArticles,
                            onDownloadImages = listViewModel::downloadUnreadImages,
                            onFinishedShown = listViewModel::clearFinished,
                            onAutoRead = listViewModel::autoMarkRead,
                            onReaderClosed = listViewModel::resetReaderSession,
                            fullTextFeedIds = fullTextFeedIds,
                            onSaveFeedSettings = listViewModel::saveFeedSettings,
                            onFetchFull = listViewModel::fetchFullContent,
                            onLoadDetail = listViewModel::loadItemDetail,
                            addFeedViewModel = addFeedViewModel,
                            onSelectScope = listViewModel::selectScope,
                            onToggleOnlyUnread = listViewModel::toggleOnlyUnread,
                            onToggleRead = listViewModel::toggleRead,
                            onToggleStarred = listViewModel::toggleStarred,
                            onSync = listViewModel::sync,
                            onLogout = listViewModel::logout,
                            onMarkAllRead = listViewModel::markAllRead,
                            onOpenSettings = { showSettings = true },
                        )
                    }
                }
            }
        }
    }
}
}
