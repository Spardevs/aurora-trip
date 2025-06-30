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

package br.com.ticpass.pos.data.room.download

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.PlayFile
import br.com.ticpass.pos.data.model.DownloadStatus
import br.com.ticpass.pos.data.room.update.Update
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
@Entity(tableName = "download")
data class Download(
    @PrimaryKey val packageName: String,
    val versionCode: Long,
    val offerType: Int,
    val isInstalled: Boolean,
    val displayName: String,
    val iconURL: String,
    val size: Long,
    val id: Int,
    var downloadStatus: DownloadStatus,
    var progress: Int,
    var speed: Long,
    var timeRemaining: Long,
    var totalFiles: Int,
    var downloadedFiles: Int,
    var fileList: List<PlayFile>,
    val sharedLibs: List<SharedLib>,
    val targetSdk: Int = 1,
    val downloadedAt: Long = 0
) : Parcelable {
    val isFinished get() = downloadStatus in DownloadStatus.finished
    val isRunning get() = downloadStatus in DownloadStatus.running

    companion object {
        fun fromApp(app: App): Download {
            return Download(
                app.packageName,
                app.versionCode,
                app.offerType,
                app.isInstalled,
                app.displayName,
                app.iconArtwork.url,
                app.size,
                app.id,
                DownloadStatus.QUEUED,
                0,
                0L,
                0L,
                0,
                0,
                app.fileList.filterNot { it.url.isBlank() },
                app.dependencies.dependentLibraries.map { SharedLib.fromApp(it) },
                app.targetSdk,
                Date().time
            )
        }

        fun fromUpdate(update: Update): Download {
            return Download(
                update.packageName,
                update.versionCode,
                update.offerType,
                true,
                update.displayName,
                update.iconURL,
                update.size,
                update.id,
                DownloadStatus.QUEUED,
                0,
                0L,
                0L,
                0,
                0,
                update.fileList,
                update.sharedLibs,
                update.targetSdk,
                Date().time
            )
        }
    }
}
