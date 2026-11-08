package app.ocnews.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {

    private val now = 1_700_000_000_000L // a fixed "now" in millis

    @Test
    fun `zero or negative returns empty`() {
        assertEquals("", formatRelativeTime(0, now))
        assertEquals("", formatRelativeTime(-5, now))
    }

    @Test
    fun `under a minute is just now`() {
        assertEquals("just now", formatRelativeTime(now / 1000 - 30, now))
    }

    @Test
    fun `minutes ago`() {
        assertEquals("5 min ago", formatRelativeTime(now / 1000 - 5 * 60, now))
    }

    @Test
    fun `hours ago`() {
        assertEquals("3 h ago", formatRelativeTime(now / 1000 - 3 * 3600, now))
    }

    @Test
    fun `days ago`() {
        assertEquals("2 days ago", formatRelativeTime(now / 1000 - 2 * 86400, now))
    }

    @Test
    fun `older than a week falls back to date`() {
        val result = formatRelativeTime(now / 1000 - 30 * 86400, now)
        // Should not be a relative string; it is a formatted date.
        org.junit.Assert.assertFalse(result.contains("ago"))
    }
}
