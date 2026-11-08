package app.ocnews.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folder")
data class FolderEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val parentId: Long? = null,
)
