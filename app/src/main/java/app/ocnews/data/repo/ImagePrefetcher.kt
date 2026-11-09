package app.ocnews.data.repo

// Prefetches an image into the disk cache so it is available offline.
fun interface ImagePrefetcher {
    suspend fun prefetch(url: String)
}
