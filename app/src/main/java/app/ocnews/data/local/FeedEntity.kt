package app.ocnews.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feed")
data class FeedEntity(
    @PrimaryKey val id: Long,
    val url: String,
    val title: String,
    val faviconLink: String,
    val added: Long,
    val nextUpdateTime: Long,
    val folderId: Long?, // null = root
    val unreadCount: Long,
    val ordering: Int,
    val link: String,
    val pinned: Boolean,
    val updateErrorCount: Int,
    val lastUpdateError: String?,
    val urlHash: String,
    val isPodcast: Boolean,
)
