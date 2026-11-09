package app.ocnews

import android.app.Application
import app.ocnews.di.AppContainer
import app.ocnews.sync.SyncScheduler
import app.ocnews.util.ImageCacheSize
import app.ocnews.util.buildImageLoader
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OcnewsApplication : Application(), SingletonImageLoader.Factory {
    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        SyncScheduler.schedule(this, 15)
        applicationScope.launch { container.migrateLegacyToken() }
        // Keep the image disk cache sized from the settings.
        applicationScope.launch {
            container.settingsStore.settings.collect { settings ->
                if (ImageCacheSize.update(settings.maxCacheSizeMb)) {
                    SingletonImageLoader.setSafe { context -> buildImageLoader(context) }
                }
            }
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = buildImageLoader(context)
}
