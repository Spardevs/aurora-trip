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

package br.com.ticpass.pos.util

import android.content.Context
import android.os.Environment
import com.aurora.gplayapi.data.models.PlayFile
import br.com.ticpass.pos.data.room.download.Download
import java.io.File
import java.util.UUID

object PathUtil {

    private const val LIBRARIES = "libraries"
    private const val DOWNLOADS = "Downloads"
    private const val SPOOF = "SpoofConfigs"

    fun getOldDownloadDirectories(context: Context): List<File> {
        return listOf(
            File(context.filesDir, DOWNLOADS), // till 4.4.2
            File(context.getExternalFilesDir(null), DOWNLOADS) // till 4.4.2
        )
    }

    fun getDownloadDirectory(context: Context): File {
        return File(context.cacheDir, DOWNLOADS)
    }

    private fun getPackageDirectory(context: Context, packageName: String): File {
        return File(getDownloadDirectory(context), packageName)
    }

    fun getAppDownloadDir(context: Context, packageName: String, versionCode: Long): File {
        return File(getPackageDirectory(context, packageName), versionCode.toString())
    }

    fun getLibDownloadDir(
        context: Context,
        packageName: String,
        versionCode: Long,
        sharedLibPackageName: String
    ): File {
        return File(
            getAppDownloadDir(context, packageName, versionCode),
            "$LIBRARIES/$sharedLibPackageName"
        )
    }

    /**
     * Returns an instance of java's [File] class for the given [PlayFile]
     * @param context [Context]
     * @param playFile [PlayFile] to download
     * @param download An instance of [Download]
     */
    fun getLocalFile(context: Context, playFile: PlayFile, download: Download): File {
        val sharedLib = download.sharedLibs.find { it.fileList.contains(playFile) }
        return when (playFile.type) {
            PlayFile.Type.BASE, PlayFile.Type.SPLIT -> {
                val downloadDir = if (sharedLib != null) {
                    getLibDownloadDir(
                        context,
                        download.packageName,
                        download.versionCode,
                        sharedLib.packageName
                    )
                } else {
                    getAppDownloadDir(context, download.packageName, download.versionCode)
                }
                return File(downloadDir, playFile.name)
            }

            PlayFile.Type.OBB, PlayFile.Type.PATCH -> {
                File(getObbDownloadDir(download.packageName), playFile.name)
            }
        }
    }

    fun getZipFile(context: Context, packageName: String, versionCode: Long): File {
        return File(
            getAppDownloadDir(
                context,
                packageName,
                versionCode,
            ), "${packageName}_${versionCode}.apks"
        )
    }

    fun getObbDownloadDir(packageName: String): File {
        return File(Environment.getExternalStorageDirectory(), "/Android/obb/$packageName")
    }

    fun getSpoofDirectory(context: Context): File {
        return File(context.filesDir, SPOOF)
    }

    fun getNewEmptySpoofConfig(context: Context): File {
        val file = File(getSpoofDirectory(context), "${UUID.randomUUID()}.properties")
        file.parentFile?.mkdirs()
        file.createNewFile()
        return file
    }
}

