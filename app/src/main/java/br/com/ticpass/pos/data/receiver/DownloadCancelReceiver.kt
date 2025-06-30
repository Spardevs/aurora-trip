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

package br.com.ticpass.pos.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import br.com.ticpass.pos.AuroraApp
import br.com.ticpass.pos.data.helper.DownloadHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DownloadCancelReceiver : BroadcastReceiver() {
    private val TAG = DownloadCancelReceiver::class.java.simpleName

    @Inject
    lateinit var downloadHelper: DownloadHelper

    override fun onReceive(context: Context, intent: Intent?) {
        val packageName: String = intent?.getStringExtra("PACKAGE_NAME") ?: ""

        if (packageName.isNotBlank()) {
            Log.d(TAG, "Received cancel download request for $packageName")
            AuroraApp.scope.launch(Dispatchers.IO) {
                downloadHelper.cancelDownload(packageName)
            }
        }
    }
}
