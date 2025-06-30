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
import android.util.Log
import br.com.ticpass.pos.AuroraApp
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.event.InstallerEvent
import br.com.ticpass.pos.data.installer.base.InstallerBase
import br.com.ticpass.pos.data.model.BuildType
import br.com.ticpass.pos.data.model.Installer
import br.com.ticpass.pos.data.model.InstallerInfo
import br.com.ticpass.pos.data.room.download.Download
import br.com.ticpass.pos.util.PackageUtil.isSharedLibraryInstalled
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootInstaller @Inject constructor(
    @ApplicationContext private val context: Context
) : InstallerBase(context) {

    companion object {
        const val PLAY_PACKAGE_NAME = "com.android.vending"

        val installerInfo: InstallerInfo
            get() = InstallerInfo(
                id = 2,
                installer = Installer.ROOT,
                packageNames = BuildType.PACKAGE_NAMES,
                installerPackageNames = listOf(PLAY_PACKAGE_NAME),
                title = R.string.pref_install_mode_root,
                subtitle = R.string.root_installer_subtitle,
                description = R.string.root_installer_desc
            )
    }

    private val TAG = RootInstaller::class.java.simpleName

    override fun install(download: Download) {
        if (isAlreadyQueued(download.packageName)) {
            Log.i(TAG, "${download.packageName} already queued")
        } else {
            if (Shell.getShell().isRoot) {
                download.sharedLibs.forEach {
                    // Shared library packages cannot be updated
                    if (!isSharedLibraryInstalled(context, it.packageName, it.versionCode)) {
                        xInstall(download.packageName, download.versionCode, it.packageName)
                    }
                }
                xInstall(download.packageName, download.versionCode)
            } else {
                postError(
                    download.packageName,
                    context.getString(R.string.installer_status_failure),
                    context.getString(R.string.installer_root_unavailable)
                )
                Log.e(TAG, " >>>>>>>>>>>>>>>>>>>>>>>>>> NO ROOT ACCESS <<<<<<<<<<<<<<<<<<<<<<<<<<<<<")
            }
        }
    }

    private fun xInstall(packageName: String, versionCode: Long, sharedLibPkgName: String = "") {
        var totalSize = 0

        for (file in getFiles(packageName, versionCode, sharedLibPkgName))
            totalSize += file.length().toInt()

        val result: Shell.Result =
            Shell.cmd("pm install-create -i $PLAY_PACKAGE_NAME --user 0 -r -S $totalSize")
                .exec()

        val response = result.out

        val sessionIdPattern = Pattern.compile("(\\d+)")
        val sessionIdMatcher = sessionIdPattern.matcher(response[0])
        val found = sessionIdMatcher.find()

        if (found) {
            val sessionId = sessionIdMatcher.group(1)?.toInt()
            if (Shell.getShell().isRoot && sessionId != null) {
                for (file in getFiles(packageName, versionCode, sharedLibPkgName)) {
                    Shell.cmd("cat \"${file.absoluteFile}\" | pm install-write -S ${file.length()} $sessionId \"${file.name}\"")
                        .exec()
                }

                val shellResult = Shell.cmd("pm install-commit $sessionId").exec()

                if (shellResult.isSuccess) {
                    // Installation is not yet finished if this is a shared library
                    if (packageName == download?.packageName) onInstallationSuccess()
                } else {
                    removeFromInstallQueue(packageName)
                    val event = InstallerEvent.Failed(packageName).apply {
                        this.extra = context.getString(R.string.installer_status_failure)
                        this.error = parseError(shellResult)
                    }
                    AuroraApp.events.send(event)
                }
            } else {
                removeFromInstallQueue(packageName)
                postError(
                    packageName,
                    context.getString(R.string.installer_status_failure),
                    context.getString(R.string.installer_root_unavailable)
                )
            }
        } else {
            removeFromInstallQueue(packageName)
            postError(
                packageName,
                context.getString(R.string.installer_status_failure),
                context.getString(R.string.installer_status_failure_session)
            )
        }
    }

    private fun parseError(result: Shell.Result): String {
        return result.err.joinToString(separator = "\n")
    }
}
