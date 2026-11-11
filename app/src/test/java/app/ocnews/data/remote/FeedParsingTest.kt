package app.ocnews.data.remote

import app.ocnews.data.remote.dto.FeedsResponse
import app.ocnews.data.toEntity
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

// The Nextcloud News app answers null for a feed that has never been fetched,
// while the OpenCloud implementation answers numbers. Both must parse.
class FeedParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val nextcloudPayload = """
        {"starredCount":67,"feeds":[
          {"id":56,"url":"https://example.com/rss","title":"Example",
           "faviconLink":null,"added":1700000000,"nextUpdateTime":null,
           "folderId":null,"unreadCount":3,"ordering":0,"link":null,
           "pinned":false,"updateErrorCount":0,"lastUpdateError":null,
           "urlHash":"abc","isPodcast":false}
        ]}
    """.trimIndent()

    @Test
    fun `null feed fields from Nextcloud are accepted`() {
        val response = json.decodeFromString<FeedsResponse>(nextcloudPayload)
        val entity = response.feeds.single().toEntity()
        assertEquals(56L, entity.id)
        assertEquals("", entity.faviconLink)
        assertEquals(0L, entity.nextUpdateTime)
        assertEquals(null, entity.folderId)
        assertEquals(3L, entity.unreadCount)
    }
}
