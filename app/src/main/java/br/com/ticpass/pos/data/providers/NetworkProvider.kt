/*
 * Copyright (c) 2025 Ticpass. All rights reserved.
 *
 * PROPRIETARY AND CONFIDENTIAL
 *
 * This software is the confidential and proprietary information of Ticpass
 * ("Confidential Information"). You shall not disclose such Confidential Information
 * and shall use it only in accordance with the terms of the license agreement you
 * entered into with Ticpass.
 *
 * Unauthorized copying, distribution, or use of this software, via any medium,
 * is strictly prohibited without the express written permission of Ticpass.
 */

package br.com.ticpass.pos.data.providers

import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import br.com.ticpass.extensions.isMAndAbove
import br.com.ticpass.extensions.isNAndAbove
import br.com.ticpass.pos.data.model.NetworkStatus
import br.com.ticpass.pos.data.receiver.NetworkBroadcastReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A simple provider with a flow to observe internet connectivity changes
 */
@Singleton
class NetworkProvider @Inject constructor(@ApplicationContext private val context: Context) {

    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!

    val status: Flow<NetworkStatus>
        get() = callbackFlow {
            if (isMAndAbove) {
                val networkCallback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        trySend(NetworkStatus.AVAILABLE).isSuccess
                    }

                    override fun onLost(network: Network) {
                        trySend(NetworkStatus.UNAVAILABLE).isSuccess
                    }
                }

                if (isNAndAbove) {
                    connectivityManager.registerDefaultNetworkCallback(networkCallback)
                } else {
                    connectivityManager.registerNetworkCallback(
                        NetworkRequest.Builder().build(),
                        networkCallback
                    )
                }

                awaitClose { connectivityManager.unregisterNetworkCallback(networkCallback) }
            } else {
                val receiver = NetworkBroadcastReceiver { isConnected ->
                    val status = if (isConnected) {
                        NetworkStatus.AVAILABLE
                    } else {
                        NetworkStatus.UNAVAILABLE
                    }
                    trySend(status).isSuccess
                }

                @Suppress("DEPRECATION")
                context.registerReceiver(
                    receiver,
                    IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
                )

                awaitClose { context.unregisterReceiver(receiver) }
            }
        }.distinctUntilChanged()
}
