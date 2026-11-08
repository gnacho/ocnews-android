package app.ocnews.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FolderEntity::class, FeedEntity::class, ItemEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class OcnewsDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun feedDao(): FeedDao
    abstract fun itemDao(): ItemDao
}
