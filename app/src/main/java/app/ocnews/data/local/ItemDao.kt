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

    @Query("SELECT * FROM item ORDER BY pubDate DESC")
    fun getAll(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM item WHERE unread = 1 ORDER BY pubDate DESC")
    fun getUnread(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM item WHERE starred = 1 ORDER BY pubDate DESC")
    fun getStarred(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM item WHERE feedId = :feedId ORDER BY pubDate DESC")
    fun getByFeed(feedId: Long): Flow<List<ItemEntity>>

    @Query("SELECT * FROM item WHERE id = :id")
    suspend fun getById(id: Long): ItemEntity?

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
