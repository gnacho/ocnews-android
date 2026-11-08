package app.ocnews.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(folders: List<FolderEntity>)

    @Query("SELECT * FROM folder ORDER BY name COLLATE NOCASE")
    fun getAll(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folder WHERE id = :id")
    suspend fun getById(id: Long): FolderEntity?

    @Query("DELETE FROM folder WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM folder")
    suspend fun clear()
}
