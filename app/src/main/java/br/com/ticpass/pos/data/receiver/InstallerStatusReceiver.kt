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

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import androidx.core.content.IntentCompat
import androidx.core.content.getSystemService
import br.com.ticpass.extensions.runOnUiThread
import br.com.ticpass.pos.AuroraApp
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.event.InstallerEvent
import br.com.ticpass.pos.data.installer.AppInstaller.Companion.ACTION_INSTALL_STATUS
import br.com.ticpass.pos.data.installer.AppInstaller.Companion.EXTRA_DISPLAY_NAME
import br.com.ticpass.pos.data.installer.AppInstaller.Companion.EXTRA_PACKAGE_NAME
import br.com.ticpass.pos.data.installer.AppInstaller.Companion.EXTRA_VERSION_CODE
import br.com.ticpass.pos.data.installer.base.InstallerBase
import br.com.ticpass.pos.util.CommonUtil.inForeground
import br.com.ticpass.pos.util.NotificationUtil
import br.com.ticpass.pos.util.PackageUtil
import br.com.ticpass.pos.util.PathUtil
import br.com.ticpass.pos.util.Preferences
import br.com.ticpass.pos.util.Preferences.PREFERENCE_AUTO_DELETE
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InstallerStatusReceiver : BroadcastReceiver() {

    private val TAG = InstallerStatusReceiver::class.java.simpleName

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent?.action == ACTION_INSTALL_STATUS) {
            val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)!!
            val displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME)!!
            val versionCode = intent.getLongExtra(EXTRA_VERSION_CODE, -1)

            val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
            val extra = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)

            // If package was successfully installed, exit after notifying user and doing cleanup
            if (status == PackageInstaller.STATUS_SUCCESS) {
                // No post-install steps for shared libraries
                if (PackageUtil.isSharedLibrary(context, packageName)) return

                AuroraApp.enqueuedInstalls.remove(packageName)
                InstallerBase.notifyInstallation(context, displayName, packageName)
                if (Preferences.getBoolean(context, PREFERENCE_AUTO_DELETE)) {
                    PathUtil.getAppDownloadDir(context, packageName, versionCode)
                        .deleteRecursively()
                }
                return
            }

            if (inForeground() && status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                promptUser(intent, context)
            } else {
                AuroraApp.enqueuedInstalls.remove(packageName)
                postStatus(status, packageName, extra, context)
                notifyUser(context, packageName, displayName, status)
            }
        }
    }

    private fun notifyUser(
        context: Context,
        packageName: String,
        displayName: String,
        status: Int
    ) {
        val notificationManager = context.getSystemService<NotificationManager>()
        val notification = NotificationUtil.getInstallerStatusNotification(
            context,
            packageName,
            displayName,
            InstallerBase.getErrorString(context, status)
        )
        notificationManager!!.notify(packageName.hashCode(), notification)
    }

    private fun promptUser(intent: Intent, context: Context) {
        IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)?.let {
            it.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            it.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, context.packageName)
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            try {
                runOnUiThread { context.startActivity(it) }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to trigger installation!", exception)
            }
        }
    }

    private fun postStatus(status: Int, packageName: String?, extra: String?, context: Context) {
        val event = when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                InstallerEvent.Installed(packageName!!).apply {
                    this.extra = context.getString(R.string.installer_status_success)
                }
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                InstallerEvent.Cancelled(packageName!!).apply {
                    this.extra = InstallerBase.getErrorString(context, status)
                }
            }

            else -> {
                InstallerEvent.Failed(packageName!!).apply {
                    this.error = InstallerBase.getErrorString(context, status)
                    this.extra = extra ?: ""
                }
            }
        }
        AuroraApp.events.send(event)
    }
}
