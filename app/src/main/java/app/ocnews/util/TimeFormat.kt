package app.ocnews.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// pubDate comes from the News API as Unix seconds. `nowMillis` is injectable for tests.
fun formatRelativeTime(epochSeconds: Long, nowMillis: Long = System.currentTimeMillis()): String {
    if (epochSeconds <= 0) return ""
    val millis = epochSeconds * 1000L
    val diff = nowMillis - millis
    return when {
        diff < 60_000L -> "just now"
        diff < 3_600_000L -> "${diff / 60_000L} min ago"
        diff < 86_400_000L -> "${diff / 3_600_000L} h ago"
        diff < 7 * 86_400_000L -> "${diff / 86_400_000L} days ago"
        else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))
    }
}
