package app.ocnews.util

import android.content.Context
import app.ocnews.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// A resolved relative-time value, ready to be formatted against string resources.
sealed interface RelativeTime {
    data object Blank : RelativeTime
    data object JustNow : RelativeTime
    data class Minutes(val n: Long) : RelativeTime
    data class Hours(val n: Long) : RelativeTime
    data class Days(val n: Long) : RelativeTime
    data class Date(val epochSeconds: Long) : RelativeTime
}

// pubDate comes from the News API as Unix seconds. `nowMillis` is injectable for tests.
// Pure Kotlin: the localized strings are applied in `RelativeTime.format`.
fun relativeTime(epochSeconds: Long, nowMillis: Long = System.currentTimeMillis()): RelativeTime {
    if (epochSeconds <= 0) return RelativeTime.Blank
    val millis = epochSeconds * 1000L
    val diff = nowMillis - millis
    return when {
        diff < 60_000L -> RelativeTime.JustNow
        diff < 3_600_000L -> RelativeTime.Minutes(diff / 60_000L)
        diff < 86_400_000L -> RelativeTime.Hours(diff / 3_600_000L)
        diff < 7 * 86_400_000L -> RelativeTime.Days(diff / 86_400_000L)
        else -> RelativeTime.Date(epochSeconds)
    }
}

fun RelativeTime.format(context: Context): String = when (this) {
    RelativeTime.Blank -> ""
    RelativeTime.JustNow -> context.getString(R.string.just_now)
    is RelativeTime.Minutes -> context.getString(R.string.min_ago, n)
    is RelativeTime.Hours -> context.getString(R.string.h_ago, n)
    is RelativeTime.Days -> context.resources.getQuantityString(R.plurals.days_ago, n.toInt(), n)
    is RelativeTime.Date ->
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(epochSeconds * 1000L))
}
