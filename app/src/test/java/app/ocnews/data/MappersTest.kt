package app.ocnews.data

import app.ocnews.data.local.toSummary
import app.ocnews.data.remote.dto.ItemDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MappersTest {

    private fun dto(body: String, mediaThumbnail: String? = null) = ItemDto(
        id = 1,
        guid = "g",
        guidHash = "h",
        url = "https://example.com/a",
        title = "Title",
        author = "Author",
        pubDate = 1_700_000_000,
        body = body,
        enclosureMime = null,
        enclosureLink = null,
        mediaThumbnail = mediaThumbnail,
        mediaDescription = null,
        feedId = 1,
        unread = true,
        starred = false,
        filtered = false,
        rtl = false,
        lastModified = 1_700_000_000,
        fingerprint = "f",
    )

    @Test
    fun `thumbnailUrl is extracted from the body at mapping time`() {
        val entity = dto("""<p>hi</p><img src="https://cdn.example.com/a.jpg">""").toEntity()
        assertEquals("https://cdn.example.com/a.jpg", entity.thumbnailUrl)
    }

    @Test
    fun `thumbnailUrl unpacks the ocnews image proxy`() {
        val proxied = "https://cloud.example.com/index.php/apps/news/api/v1-3/img?u=" +
            java.net.URLEncoder.encode("https://cdn.example.com/b.jpg?x=1&y=2", "UTF-8")
        val entity = dto("""<img src="$proxied">""").toEntity()
        assertEquals("https://cdn.example.com/b.jpg?x=1&y=2", entity.thumbnailUrl)
    }

    @Test
    fun `thumbnailUrl is null without images`() {
        assertNull(dto("<p>no images here</p>").toEntity().thumbnailUrl)
    }

    @Test
    fun `summary falls back from media thumbnail to body image`() {
        val withMedia = dto("<p>x</p>", mediaThumbnail = "https://cdn.example.com/m.jpg").toEntity()
            .toSummary()
        assertEquals("https://cdn.example.com/m.jpg", withMedia.imageUrl)

        val bodyOnly = dto("""<img src="https://cdn.example.com/a.jpg">""").toEntity().toSummary()
        assertEquals("https://cdn.example.com/a.jpg", bodyOnly.imageUrl)

        val none = dto("<p>x</p>").toEntity().toSummary()
        assertNull(none.imageUrl)
    }
}
