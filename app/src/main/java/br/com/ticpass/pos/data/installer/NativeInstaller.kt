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

package br.com.ticpass.pos.data.installer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import br.com.ticpass.extensions.runOnUiThread
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.installer.base.InstallerBase
import br.com.ticpass.pos.data.model.BuildType
import br.com.ticpass.pos.data.model.Installer
import br.com.ticpass.pos.data.model.InstallerInfo
import br.com.ticpass.pos.data.room.download.Download
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Deprecated("Deprecated in favour of SessionInstaller")
class NativeInstaller @Inject constructor(
    @ApplicationContext private val context: Context
) : InstallerBase(context) {

    companion object {

        val installerInfo: InstallerInfo
            get() = InstallerInfo(
                id = 1,
                installer = Installer.NATIVE,
                packageNames = BuildType.PACKAGE_NAMES,
                installerPackageNames = BuildType.PACKAGE_NAMES,
                title = R.string.pref_install_mode_native,
                subtitle = R.string.native_installer_subtitle,
                description = R.string.native_installer_desc
            )
    }

    private val TAG = NativeInstaller::class.java.simpleName

    override fun install(download: Download) {
        if (isAlreadyQueued(download.packageName)) {
            Log.i(TAG, "${download.packageName} already queued")
        } else {
            Log.i(TAG, "Received native install request for ${download.packageName}")
            getFiles(download.packageName, download.versionCode).forEach { xInstall(it) }
        }
    }

    private fun xInstall(file: File) {
        val intent: Intent

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            @Suppress("DEPRECATION")
            intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
            intent.data = getUri(file)
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        } else {
            intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        intent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, context.packageName)
        runOnUiThread { context.startActivity(intent) }
    }
}
