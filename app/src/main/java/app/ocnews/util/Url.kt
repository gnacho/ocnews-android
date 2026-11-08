package app.ocnews.util

// Resolves a possibly-relative server URL (e.g. a favicon path returned by the
// News API) against the configured server. Absolute URLs pass through unchanged.
fun resolveUrl(serverUrl: String, path: String): String {
    if (path.isBlank()) return path
    if (path.startsWith("http://") || path.startsWith("https://")) return path
    return serverUrl.trimEnd('/') + path
}
