package app.ocnews.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "item",
    indices = [
        Index("feedId"),
        Index("unread"),
        Index("starred"),
        Index("lastModified"),
    ],
)
data class ItemEntity(
    @PrimaryKey val id: Long,
    val guid: String,
    val guidHash: String,
    val url: String,
    val title: String,
    val author: String,
    val pubDate: Long,
    val body: String,
    val enclosureMime: String?,
    val enclosureLink: String?,
    val mediaThumbnail: String?,
    val mediaDescription: String?,
    val feedId: Long,
    val unread: Boolean,
    val starred: Boolean,
    val filtered: Boolean,
    val rtl: Boolean,
    val lastModified: Long,
    val fingerprint: String,
    // Outbound queue: true when the effective value differs from the server.
    val unreadPending: Boolean = false,
    val starredPending: Boolean = false,
)
