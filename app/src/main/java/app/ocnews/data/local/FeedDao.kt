package app.ocnews.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(feeds: List<FeedEntity>)

    @Query("SELECT * FROM feed ORDER BY title COLLATE NOCASE")
    fun getAll(): Flow<List<FeedEntity>>

    @Query("SELECT * FROM feed WHERE folderId = :folderId")
    fun getByFolder(folderId: Long): Flow<List<FeedEntity>>

    @Query("SELECT * FROM feed WHERE folderId IS NULL")
    fun getRootFeeds(): Flow<List<FeedEntity>>

    @Query("SELECT * FROM feed WHERE id = :id")
    suspend fun getById(id: Long): FeedEntity?

    @Query("UPDATE feed SET title = :title WHERE id = :id")
    suspend fun rename(id: Long, title: String)

    @Query("UPDATE feed SET folderId = :folderId WHERE id = :id")
    suspend fun move(id: Long, folderId: Long?)

    @Query("DELETE FROM feed WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM feed")
    suspend fun clear()
}
