package app.ocnews.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlTest {

    @Test
    fun `relative path resolves against server`() {
        assertEquals(
            "https://host/index.php/apps/news/api/v1-3/favicon/abc",
            resolveUrl("https://host", "/index.php/apps/news/api/v1-3/favicon/abc"),
        )
    }

    @Test
    fun `server url trailing slash is trimmed`() {
        assertEquals(
            "https://host/favicon/abc",
            resolveUrl("https://host/", "/favicon/abc"),
        )
    }

    @Test
    fun `absolute url passes through unchanged`() {
        assertEquals(
            "https://other/img.png",
            resolveUrl("https://host", "https://other/img.png"),
        )
    }

    @Test
    fun `blank path stays blank`() {
        assertEquals("", resolveUrl("https://host", ""))
    }
}
