package br.com.ticpass.pos

import android.annotation.SuppressLint
import android.app.Application
import android.os.Handler
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import br.com.ticpass.pos.core.util.ConnectionStatusBar
import br.com.ticpass.pos.core.util.ConnectivityMonitor
import br.com.ticpass.pos.core.util.SessionPrefsManagerUtils
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MainApplication : Application(), Configuration.Provider {

    companion object {
        lateinit var instance: MainApplication
            private set

        @SuppressLint("StaticFieldLeak")
        lateinit var connectivityMonitor: ConnectivityMonitor
        @SuppressLint("StaticFieldLeak")
        lateinit var connectionStatusBar: ConnectionStatusBar
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        instance = this

        val db = br.com.ticpass.pos.data.local.database.AppDatabase.getDatabase(this)
        val posDaoInstance = db.posDao()
        br.com.ticpass.pos.core.util.CommisionUtils.setPosDao(posDaoInstance)

        if (BuildConfig.DEBUG) {
            SessionPrefsManagerUtils.init(this)
            Timber.plant(Timber.DebugTree())
        }

        connectionStatusBar = ConnectionStatusBar(applicationContext)
        connectivityMonitor = ConnectivityMonitor(applicationContext, Handler(mainLooper)).apply {
            onConnectionChanged = { isConnected ->
                connectionStatusBar.show(isConnected)
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}