package app.ocnews.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {

    private val now = 1_700_000_000_000L // a fixed "now" in millis

    @Test
    fun `zero or negative is blank`() {
        assertEquals(RelativeTime.Blank, relativeTime(0, now))
        assertEquals(RelativeTime.Blank, relativeTime(-5, now))
    }

    @Test
    fun `under a minute is just now`() {
        assertEquals(RelativeTime.JustNow, relativeTime(now / 1000 - 30, now))
    }

    @Test
    fun `minutes ago`() {
        assertEquals(RelativeTime.Minutes(5), relativeTime(now / 1000 - 5 * 60, now))
    }

    @Test
    fun `hours ago`() {
        assertEquals(RelativeTime.Hours(3), relativeTime(now / 1000 - 3 * 3600, now))
    }

    @Test
    fun `days ago`() {
        assertEquals(RelativeTime.Days(2), relativeTime(now / 1000 - 2 * 86400, now))
    }

    @Test
    fun `older than a week falls back to date`() {
        val epoch = now / 1000 - 30 * 86400
        assertEquals(RelativeTime.Date(epoch), relativeTime(epoch, now))
    }
}
