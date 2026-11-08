package app.ocnews.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.ocnews.OcnewsApplication

// Periodic sync worker: drains pending state and pulls updates.
class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as OcnewsApplication).container
        return try {
            container.repository.sync()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
