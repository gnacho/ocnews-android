package app.ocnews.util

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import okio.Path.Companion.toOkioPath

// The disk cache size comes from the settings, so the loader is rebuilt when it
// changes. Coil's singleton asks this object for the current value.
object ImageCacheSize {
    const val DEFAULT_MB = 500

    @Volatile
    var mb: Int = DEFAULT_MB
        private set

    // Returns true when the size actually changed.
    fun update(newMb: Int): Boolean {
        val safe = if (newMb <= 0) DEFAULT_MB else newMb
        val changed = safe != mb
        mb = safe
        return changed
    }
}

fun buildImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
    .components { add(OkHttpNetworkFetcherFactory()) }
    .diskCache {
        DiskCache.Builder()
            .directory(context.cacheDir.resolve("image_cache").toOkioPath())
            .maxSizeBytes(ImageCacheSize.mb * 1024L * 1024L)
            .build()
    }
    .crossfade(true)
    .build()
