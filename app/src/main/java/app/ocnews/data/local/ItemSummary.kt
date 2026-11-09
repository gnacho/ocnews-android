package app.ocnews.data.local

// Lightweight projection for the article list: everything the list rows and the
// reader header need, but without the HTML body (which can be several KB per item).
data class ItemSummary(
    val id: Long,
    val url: String,
    val title: String,
    val author: String,
    val pubDate: Long,
    val feedId: Long,
    val unread: Boolean,
    val starred: Boolean,
    val rtl: Boolean,
    val mediaThumbnail: String?,
    val thumbnailUrl: String?,
) {
    // Hero/thumbnail: the feed-provided media thumbnail wins, then the first
    // image extracted from the body at sync time.
    val imageUrl: String? get() = mediaThumbnail ?: thumbnailUrl?.takeIf { it.isNotBlank() }
}

fun ItemEntity.toSummary(): ItemSummary = ItemSummary(
    id = id,
    url = url,
    title = title,
    author = author,
    pubDate = pubDate,
    feedId = feedId,
    unread = unread,
    starred = starred,
    rtl = rtl,
    mediaThumbnail = mediaThumbnail,
    thumbnailUrl = thumbnailUrl,
)

// Row used to keep the offline full text when a sync replaces the item.
data class ItemFullBody(
    val id: Long,
    val fullBody: String,
)

// Row used to backfill the thumbnail after a migration.
data class ItemBodyRow(
    val id: Long,
    val body: String,
)
