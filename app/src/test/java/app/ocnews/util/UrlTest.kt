package app.ocnews.util

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Assert.assertEquals

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

    @Test
    fun `normalizeServerUrl adds https and drops trailing slashes`() {
        assertEquals("https://cloud.example.com", normalizeServerUrl("cloud.example.com"))
        assertEquals("https://cloud.example.com", normalizeServerUrl("  cloud.example.com/  "))
        assertEquals("https://cloud.example.com", normalizeServerUrl("https://cloud.example.com/"))
        assertEquals("http://192.168.1.10:8080", normalizeServerUrl("http://192.168.1.10:8080"))
        assertEquals("", normalizeServerUrl("   "))
    }
}
