package br.com.ticpass.pos.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import br.com.ticpass.pos.R

class ConnectivityMonitor(
    private val context: Context,
    private val handler: Handler
) {
    private var isConnected: Boolean? = null

    var onConnectionChanged: ((Boolean) -> Unit)? = null

    fun checkCurrentStatus() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val connected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnectedOrConnecting ?: false
        }

        updateConnectionStatus(connected)
    }

    private fun updateConnectionStatus(connected: Boolean) {
        if (isConnected != connected) {
            isConnected = connected
            handler.post {
                onConnectionChanged?.invoke(connected)
            }
        }
    }

    fun cleanup() {
        // Limpeza de recursos se necessário
    }
}