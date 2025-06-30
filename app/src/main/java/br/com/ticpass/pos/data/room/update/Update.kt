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

package br.com.ticpass.pos.data.room.update

import android.content.Context
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.PlayFile
import br.com.ticpass.pos.data.room.download.SharedLib
import br.com.ticpass.pos.util.CertUtil
import br.com.ticpass.pos.util.PackageUtil
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "update")
data class Update(
    @PrimaryKey
    val packageName: String,
    val versionCode: Long,
    val versionName: String,
    val displayName: String,
    val iconURL: String,
    val changelog: String,
    val id: Int,
    val developerName: String,
    val size: Long,
    val updatedOn: String,
    val hasValidCert: Boolean,
    val offerType: Int,
    var fileList: List<PlayFile>,
    val sharedLibs: List<SharedLib>,
    val targetSdk: Int = 1
) : Parcelable {

    companion object {
        fun fromApp(context: Context, app: App): Update {
            return Update(
                app.packageName,
                app.versionCode,
                app.versionName,
                app.displayName,
                app.iconArtwork.url,
                app.changes,
                app.id,
                app.developerName,
                app.size,
                app.updatedOn,
                app.certificateSetList.any {
                    it.certificateSet in CertUtil.getEncodedCertificateHashes(
                        context, app.packageName
                    )
                },
                app.offerType,
                app.fileList.filterNot { it.url.isBlank() },
                app.dependencies.dependentLibraries.map { SharedLib.fromApp(it) },
                app.targetSdk
            )
        }
    }

    fun isSelfUpdate(context: Context): Boolean {
        return packageName == context.packageName
    }

    fun isInstalled(context: Context): Boolean {
        return PackageUtil.isInstalled(context, packageName)
    }

    fun isUpToDate(context: Context): Boolean {
        return PackageUtil.isInstalled(context, packageName, versionCode)
    }
}
