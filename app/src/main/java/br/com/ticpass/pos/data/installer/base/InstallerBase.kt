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

package br.com.ticpass.pos.data.installer.base

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageInstaller
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import br.com.ticpass.pos.AuroraApp
import br.com.ticpass.pos.BuildConfig
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.event.InstallerEvent
import br.com.ticpass.pos.data.room.download.Download
import br.com.ticpass.pos.util.NotificationUtil
import br.com.ticpass.pos.util.PathUtil
import br.com.ticpass.pos.util.Preferences
import br.com.ticpass.pos.util.Preferences.PREFERENCE_AUTO_DELETE
import java.io.File

abstract class InstallerBase(private val context: Context) : IInstaller {

    companion object {
        fun notifyInstallation(context: Context, displayName: String, packageName: String) {
            val notificationManager = context.getSystemService<NotificationManager>()
            val notification = NotificationUtil.getInstallNotification(context, displayName, packageName)
            notificationManager!!.notify(packageName.hashCode(), notification)
        }

        fun getErrorString(context: Context, status: Int): String {
            return when (status) {
                PackageInstaller.STATUS_FAILURE_ABORTED -> context.getString(R.string.installer_status_user_action)
                PackageInstaller.STATUS_FAILURE_BLOCKED -> context.getString(R.string.installer_status_failure_blocked)
                PackageInstaller.STATUS_FAILURE_CONFLICT -> context.getString(R.string.installer_status_failure_conflict)
                PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> context.getString(R.string.installer_status_failure_incompatible)
                PackageInstaller.STATUS_FAILURE_INVALID -> context.getString(R.string.installer_status_failure_invalid)
                PackageInstaller.STATUS_FAILURE_STORAGE -> context.getString(R.string.installer_status_failure_storage)
                else -> context.getString(R.string.installer_status_failure)
            }
        }
    }

    private val TAG = InstallerBase::class.java.simpleName

    var download: Download? = null
        private set

    override fun install(download: Download) {
        this.download = download
    }

    override fun clearQueue() {
        AuroraApp.enqueuedInstalls.clear()
    }

    override fun isAlreadyQueued(packageName: String): Boolean {
        return AuroraApp.enqueuedInstalls.contains(packageName)
    }

    override fun removeFromInstallQueue(packageName: String) {
        AuroraApp.enqueuedInstalls.remove(packageName)
    }

    fun onInstallationSuccess() {
        download?.let {
            notifyInstallation(context, it.displayName, it.packageName)
            if (Preferences.getBoolean(context, PREFERENCE_AUTO_DELETE)) {
                PathUtil.getAppDownloadDir(context, it.packageName, it.versionCode)
                    .deleteRecursively()
            }
        }
    }

    open fun postError(packageName: String, error: String?, extra: String?) {
        Log.e(TAG, "Service Error :$error")

        val event = InstallerEvent.Failed(packageName).apply {
            this.error = error ?: ""
            this.extra = extra ?: ""
        }

        AuroraApp.events.send(event)
    }

    fun getFiles(
        packageName: String,
        versionCode: Long,
        sharedLibPackageName: String = ""
    ): List<File> {
        val downloadDir = if (sharedLibPackageName.isNotBlank()) {
            PathUtil.getLibDownloadDir(context, packageName, versionCode, sharedLibPackageName)
        } else {
            PathUtil.getAppDownloadDir(context, packageName, versionCode)
        }
        return downloadDir.listFiles()!!.filter { it.path.endsWith(".apk") }
    }

    fun getUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileProvider",
            file
        )
    }
}
