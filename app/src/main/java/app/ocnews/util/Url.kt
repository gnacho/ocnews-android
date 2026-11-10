package app.ocnews.util

// Resolves a possibly-relative server URL (e.g. a favicon path returned by the
// News API) against the configured server. Absolute URLs pass through unchanged.
fun resolveUrl(serverUrl: String, path: String): String {
    if (path.isBlank()) return path
    if (path.startsWith("http://") || path.startsWith("https://")) return path
    return serverUrl.trimEnd('/') + path
}

// Users should not have to type the scheme. Anything without http(s) is assumed
// to be https, and trailing slashes are dropped.
fun normalizeServerUrl(raw: String): String {
    val trimmed = raw.trim().trimEnd('/')
    if (trimmed.isEmpty()) return ""
    return when {
        trimmed.startsWith("http://", ignoreCase = true) -> trimmed
        trimmed.startsWith("https://", ignoreCase = true) -> trimmed
        else -> "https://$trimmed"
    }
}
