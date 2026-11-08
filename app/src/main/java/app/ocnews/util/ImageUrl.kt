package app.ocnews.util

// Extracts the first <img src> from an article body.
fun firstImageUrl(html: String): String? {
    val regex = Regex("""<img[^>]+src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    return regex.find(html)?.groupValues?.getOrNull(1)
}

// ocnews rewrites article images to its own proxy (/img?u=<encoded>), which returns 403.
// Unpack the original image URL from the `u` parameter (also decodes &amp;).
fun unpackImageUrl(src: String): String {
    val decoded = src.replace("&amp;", "&")
    if (!decoded.contains("/img?")) return decoded
    val u = Regex("""[?&]u=([^&]+)""").find(decoded)?.groupValues?.getOrNull(1) ?: return decoded
    return try {
        java.net.URLDecoder.decode(u, "UTF-8")
    } catch (e: Exception) {
        decoded
    }
}
