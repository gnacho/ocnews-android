package app.ocnews.ui.list

import app.ocnews.data.local.ItemSummary
import app.ocnews.data.settings.SwipeAction

enum class SwipeOutcome { NONE, MARK_READ, MARK_UNREAD, STAR }

// READ toggles the read flag in both directions. STAR only marks, so a stray
// swipe never takes a star away from the user.
fun resolveSwipe(action: SwipeAction, item: ItemSummary): SwipeOutcome = when (action) {
    SwipeAction.READ -> if (item.unread) SwipeOutcome.MARK_READ else SwipeOutcome.MARK_UNREAD
    SwipeAction.STAR -> if (item.starred) SwipeOutcome.NONE else SwipeOutcome.STAR
    SwipeAction.NOTHING -> SwipeOutcome.NONE
}
