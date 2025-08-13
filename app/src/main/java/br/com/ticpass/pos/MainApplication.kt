package br.com.ticpass.pos

import android.app.Application
import android.os.Handler
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import br.com.ticpass.pos.util.ConnectivityMonitor
import br.com.ticpass.pos.util.ConnectionStatusBar
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MainApplication : Application(), Configuration.Provider {

    companion object {
        lateinit var instance: MainApplication
            private set

        lateinit var connectivityMonitor: ConnectivityMonitor
        lateinit var connectionStatusBar: ConnectionStatusBar
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        instance = this

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