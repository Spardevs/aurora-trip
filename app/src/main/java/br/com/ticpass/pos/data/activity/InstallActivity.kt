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

package br.com.ticpass.pos.data.activity

import android.content.pm.PackageInstaller.SessionCallback
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import br.com.ticpass.Constants
import br.com.ticpass.pos.data.installer.SessionInstaller
import br.com.ticpass.pos.data.room.download.Download
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class InstallActivity : AppCompatActivity() {

    private val TAG = InstallActivity::class.java.simpleName

    @Inject
    lateinit var sessionInstaller: SessionInstaller

    private lateinit var sessionCallback: SessionCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val download =
            IntentCompat.getParcelableExtra(intent, Constants.PARCEL_DOWNLOAD, Download::class.java)

        if (download != null) {
            sessionCallback = object : SessionCallback() {
                override fun onCreated(sessionId: Int) {}

                override fun onBadgingChanged(sessionId: Int) {}

                override fun onActiveChanged(sessionId: Int, active: Boolean) {}

                override fun onProgressChanged(sessionId: Int, progress: Float) {}

                override fun onFinished(sessionId: Int, success: Boolean) {
                    if (sessionInstaller.currentSessionId == sessionId) {
                        Log.i(TAG, "Install finished with status code: $success")
                        finish()
                    }
                }
            }
            packageManager.packageInstaller.registerSessionCallback(sessionCallback)
            install(download)
        } else {
            Log.e(TAG, "InstallActivity triggered without a valid download, bailing out!")
            finish()
        }
    }

    override fun onDestroy() {
        packageManager.packageInstaller.unregisterSessionCallback(sessionCallback)
        super.onDestroy()
    }

    private fun install(download: Download) {
        try {
            sessionInstaller.install(download)
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to install $packageName")
            finish()
        }
    }
}
