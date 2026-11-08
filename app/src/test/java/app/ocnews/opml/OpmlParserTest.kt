package app.ocnews.opml

import org.junit.Assert.assertEquals
import org.junit.Test

class OpmlParserTest {

    @Test
    fun `parses flat feeds`() {
        val xml = """
            <opml version="2.0"><body>
              <outline type="rss" text="A" xmlUrl="https://a.example.com/feed"/>
              <outline type="rss" text="B" xmlUrl="https://b.example.com/feed"/>
            </body></opml>
        """.trimIndent()

        val entries = OpmlParser.parse(xml)

        assertEquals(
            listOf(
                OpmlEntry("https://a.example.com/feed", null),
                OpmlEntry("https://b.example.com/feed", null),
            ),
            entries,
        )
    }

    @Test
    fun `parses nested folders`() {
        val xml = """
            <opml version="2.0"><body>
              <outline text="Tech" title="Tech">
                <outline type="rss" text="A" xmlUrl="https://a.example.com/feed"/>
                <outline type="rss" text="B" xmlUrl="https://b.example.com/feed"/>
              </outline>
              <outline text="News" title="News">
                <outline type="rss" text="C" xmlUrl="https://c.example.com/feed"/>
              </outline>
            </body></opml>
        """.trimIndent()

        val entries = OpmlParser.parse(xml)

        assertEquals(
            listOf(
                OpmlEntry("https://a.example.com/feed", "Tech"),
                OpmlEntry("https://b.example.com/feed", "Tech"),
                OpmlEntry("https://c.example.com/feed", "News"),
            ),
            entries,
        )
    }

    @Test
    fun `ignores non-outline and non-feed nodes`() {
        val xml = """
            <opml version="2.0">
              <head><title>My subs</title></head>
              <body>
                <outline text="Tech" title="Tech">
                  <outline type="rss" text="A" xmlUrl="https://a.example.com/feed"/>
                </outline>
              </body>
            </opml>
        """.trimIndent()

        val entries = OpmlParser.parse(xml)

        assertEquals(1, entries.size)
        assertEquals(OpmlEntry("https://a.example.com/feed", "Tech"), entries.first())
    }
}
