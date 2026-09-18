package ch.ncavallini.polywear

import android.app.Application
import ch.ncavallini.polywear.di.AppContainer

/** Holds the app-wide [AppContainer]; also reached by the Data Layer service. */
class PolyApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
