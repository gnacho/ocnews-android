package app.ocnews.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ItemEntity>)

    // Lightweight list queries: no body column, so a toggle does not reload
    // megabytes of article HTML. The reader loads the full row on demand.
    @Query("SELECT id, url, title, author, pubDate, feedId, unread, starred, rtl, mediaThumbnail, thumbnailUrl FROM item ORDER BY pubDate DESC")
    fun getAllSummaries(): Flow<List<ItemSummary>>

    @Query("SELECT id, url, title, author, pubDate, feedId, unread, starred, rtl, mediaThumbnail, thumbnailUrl FROM item WHERE unread = 1 ORDER BY pubDate DESC")
    fun getUnreadSummaries(): Flow<List<ItemSummary>>

    @Query("SELECT id, url, title, author, pubDate, feedId, unread, starred, rtl, mediaThumbnail, thumbnailUrl FROM item WHERE starred = 1 ORDER BY pubDate DESC")
    fun getStarredSummaries(): Flow<List<ItemSummary>>

    @Query("SELECT id, url, title, author, pubDate, feedId, unread, starred, rtl, mediaThumbnail, thumbnailUrl FROM item WHERE feedId = :feedId ORDER BY pubDate DESC")
    fun getByFeedSummaries(feedId: Long): Flow<List<ItemSummary>>

    @Query("SELECT * FROM item WHERE id = :id")
    suspend fun getById(id: Long): ItemEntity?

    // Offline download helpers: unread scope only.
    @Query("SELECT id FROM item WHERE unread = 1")
    suspend fun getUnreadIds(): List<Long>

    @Query(
        "SELECT COALESCE(mediaThumbnail, thumbnailUrl) FROM item " +
            "WHERE unread = 1 AND COALESCE(mediaThumbnail, thumbnailUrl, '') != ''"
    )
    suspend fun getUnreadImageUrls(): List<String>

    @Query("UPDATE item SET fullBody = :body WHERE id = :id")
    suspend fun setFullBody(id: Long, body: String?)

    @Query("SELECT id, fullBody FROM item WHERE id IN (:ids) AND fullBody IS NOT NULL")
    suspend fun getFullBodies(ids: List<Long>): List<ItemFullBody>

    // Full-text match in the body returns only ids, keeping HTML out of memory.
    @Query("SELECT id FROM item WHERE body LIKE '%' || :query || '%'")
    suspend fun searchBodyIds(query: String): List<Long>

    @Query("SELECT id, body FROM item WHERE thumbnailUrl IS NULL AND body LIKE '%<img%' LIMIT :limit")
    suspend fun getItemsMissingThumbnail(limit: Int): List<ItemBodyRow>

    @Query("UPDATE item SET thumbnailUrl = :thumbnailUrl WHERE id = :id")
    suspend fun setThumbnailUrl(id: Long, thumbnailUrl: String?)

    @Query("SELECT MAX(lastModified) FROM item")
    suspend fun getHighestLastModified(): Long?

    @Query("SELECT COUNT(*) FROM item WHERE unread = 1")
    fun observeUnreadCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM item WHERE starred = 1")
    fun observeStarredCount(): Flow<Int>

    // State changes (optimistic): flip the effective value and flag as pending.
    @Query("UPDATE item SET unread = :unread, unreadPending = 1 WHERE id = :id")
    suspend fun setUnread(id: Long, unread: Boolean)

    @Query("UPDATE item SET unread = 0, unreadPending = 0")
    suspend fun markAllRead()

    @Query("UPDATE item SET starred = :starred, starredPending = 1 WHERE id = :id")
    suspend fun setStarred(id: Long, starred: Boolean)

    // Outbound queue draining.
    @Query("SELECT * FROM item WHERE unreadPending = 1")
    suspend fun getPendingUnread(): List<ItemEntity>

    @Query("SELECT * FROM item WHERE starredPending = 1")
    suspend fun getPendingStarred(): List<ItemEntity>

    @Query("UPDATE item SET unreadPending = 0 WHERE id IN (:ids)")
    suspend fun clearUnreadPending(ids: List<Long>)

    @Query("UPDATE item SET starredPending = 0 WHERE id IN (:ids)")
    suspend fun clearStarredPending(ids: List<Long>)

    @Query("DELETE FROM item")
    suspend fun clear()
}
