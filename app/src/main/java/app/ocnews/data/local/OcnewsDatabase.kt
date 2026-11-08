package app.ocnews.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FolderEntity::class, FeedEntity::class, ItemEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class OcnewsDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun feedDao(): FeedDao
    abstract fun itemDao(): ItemDao

    companion object {
        // v2 adds the thumbnailUrl column so list queries can skip the body.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE item ADD COLUMN thumbnailUrl TEXT")
            }
        }
    }
}
