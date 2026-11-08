package app.ocnews.sync

import app.ocnews.data.local.FeedDao
import app.ocnews.data.local.FolderDao
import app.ocnews.data.local.ItemDao
import app.ocnews.data.remote.NewsApi
import app.ocnews.data.remote.dto.MarkMultipleRequest
import app.ocnews.data.toEntity

// Orchestrates the v1.3 sync protocol against the local DAOs.
// Pure Kotlin: testable in the JVM with fakes.
class SyncEngine(
    private val api: NewsApi,
    private val folderDao: FolderDao,
    private val feedDao: FeedDao,
    private val itemDao: ItemDao,
) {
    suspend fun initialSync() {
        val folders = api.folders()
        val feeds = api.feeds()
        folderDao.upsertAll(folders.folders.map { it.toEntity() })
        feedDao.upsertAll(feeds.feeds.map { it.toEntity() })

        // Unread items: type=3 (all), getRead=false, batchSize=-1, oldest first.
        val unread = api.items(type = 3, getRead = false, batchSize = -1, oldestFirst = true)
        itemDao.upsertAll(unread.items.map { it.toEntity() })

        // Starred items (may include read ones).
        val starred = api.items(type = 2, getRead = true, batchSize = -1, oldestFirst = true)
        itemDao.upsertAll(starred.items.map { it.toEntity() })
    }

    suspend fun incrementalSync() {
        val watermark = itemDao.getHighestLastModified() ?: 0L
        val updated = api.itemsUpdated(lastModified = watermark)
        itemDao.upsertAll(updated.items.map { it.toEntity() })
    }

    // Drains pending read/unread and star/unstar changes to the server.
    // Pending flags are cleared only after a successful server ack.
    suspend fun pushPendingState() {
        val unreadPending = itemDao.getPendingUnread()
        val (toUnread, toRead) = unreadPending.partition { it.unread }
        for (batch in toUnread.map { it.id }.chunked(BATCH_SIZE)) {
            api.markUnreadMultiple(MarkMultipleRequest(batch))
            itemDao.clearUnreadPending(batch)
        }
        for (batch in toRead.map { it.id }.chunked(BATCH_SIZE)) {
            api.markReadMultiple(MarkMultipleRequest(batch))
            itemDao.clearUnreadPending(batch)
        }

        val starredPending = itemDao.getPendingStarred()
        val (toStar, toUnstar) = starredPending.partition { it.starred }
        for (batch in toStar.map { it.id }.chunked(BATCH_SIZE)) {
            api.markStarMultiple(MarkMultipleRequest(batch))
            itemDao.clearStarredPending(batch)
        }
        for (batch in toUnstar.map { it.id }.chunked(BATCH_SIZE)) {
            api.markUnstarMultiple(MarkMultipleRequest(batch))
            itemDao.clearStarredPending(batch)
        }
    }

    companion object {
        const val BATCH_SIZE = 300
    }
}
