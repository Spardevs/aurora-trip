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
import android.util.Log
import br.com.ticpass.pos.R
import br.com.ticpass.pos.data.installer.base.InstallerBase
import br.com.ticpass.pos.data.model.Installer
import br.com.ticpass.pos.data.model.InstallerInfo
import br.com.ticpass.pos.data.room.download.Download
import br.com.ticpass.pos.util.PackageUtil.isSharedLibraryInstalled
import br.com.ticpass.pos.util.PathUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AMInstaller @Inject constructor(
    @ApplicationContext private val context: Context
) : InstallerBase(context) {

    companion object {
        const val AM_PACKAGE_NAME = "io.github.muntashirakon.AppManager"
        const val AM_DEBUG_PACKAGE_NAME = "io.github.muntashirakon.AppManager.debug"

        val installerInfo: InstallerInfo
            get() = InstallerInfo(
                id = 4,
                installer = Installer.AM,
                packageNames = listOf(AM_PACKAGE_NAME, AM_DEBUG_PACKAGE_NAME),
                installerPackageNames = listOf(AM_PACKAGE_NAME, AM_DEBUG_PACKAGE_NAME),
                title = R.string.pref_install_mode_am,
                subtitle = R.string.am_installer_subtitle,
                description = R.string.am_installer_desc
            )
    }

    private val TAG = AMInstaller::class.java.simpleName

    override fun install(download: Download) {
        if (isAlreadyQueued(download.packageName)) {
            Log.i(TAG, "${download.packageName} already queued")
        } else {
            Log.i(TAG, "Received AM install request for ${download.packageName}")
            val fileList = mutableListOf<File>()

            download.sharedLibs.forEach {
                // Shared library packages cannot be updated
                if (!isSharedLibraryInstalled(context, it.packageName, it.versionCode)) {
                    fileList.addAll(
                        getFiles(
                            download.packageName,
                            download.versionCode,
                            it.packageName
                        )
                    )
                }
            }

            val zipFile = PathUtil.getZipFile(context, download.packageName, download.versionCode)
            fileList.add(zip(getFiles(download.packageName, download.versionCode), zipFile))

            install(fileList)
        }
    }

    private fun install(files: List<File>) {
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "application/x-apks"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(files.map { getUri(it) }))
        }
        context.startActivity(intent)
    }

    fun zip(files: List<File>, zipFile: File): File {
        ZipOutputStream(zipFile.outputStream()).use { zipOutput ->
            files.forEach { file ->
                file.inputStream().use { input ->
                    zipOutput.putNextEntry(ZipEntry(file.name))
                    input.copyTo(zipOutput)
                }
            }
        }
        return zipFile
    }
}
