package app.ocnews.ui.list

import app.ocnews.data.local.ItemSummary
import app.ocnews.data.settings.SwipeAction
import org.junit.Assert.assertEquals
import org.junit.Test

class SwipeOutcomeTest {

    private fun item(unread: Boolean, starred: Boolean) = ItemSummary(
        id = 1L,
        url = "https://example.com/1",
        title = "Title",
        author = "",
        pubDate = 0L,
        feedId = 1L,
        unread = unread,
        starred = starred,
        rtl = false,
        mediaThumbnail = null,
        thumbnailUrl = null,
    )

    @Test
    fun `read swipe marks an unread item as read`() {
        assertEquals(SwipeOutcome.MARK_READ, resolveSwipe(SwipeAction.READ, item(unread = true, starred = false)))
    }

    @Test
    fun `read swipe marks a read item as unread`() {
        assertEquals(SwipeOutcome.MARK_UNREAD, resolveSwipe(SwipeAction.READ, item(unread = false, starred = false)))
    }

    @Test
    fun `star swipe marks an unstarred item`() {
        assertEquals(SwipeOutcome.STAR, resolveSwipe(SwipeAction.STAR, item(unread = false, starred = false)))
    }

    @Test
    fun `star swipe leaves an already starred item alone`() {
        assertEquals(SwipeOutcome.NONE, resolveSwipe(SwipeAction.STAR, item(unread = false, starred = true)))
    }

    @Test
    fun `nothing does nothing`() {
        assertEquals(SwipeOutcome.NONE, resolveSwipe(SwipeAction.NOTHING, item(unread = true, starred = false)))
    }
}
