package app.ocnews

import android.app.Application
import app.ocnews.di.AppContainer
import app.ocnews.sync.SyncScheduler

class OcnewsApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        SyncScheduler.schedule(this)
    }
}
