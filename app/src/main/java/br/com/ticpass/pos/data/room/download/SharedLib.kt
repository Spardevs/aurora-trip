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
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.PlayFile
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class SharedLib(
    val packageName: String,
    val versionCode: Long,
    var fileList: List<PlayFile>
) : Parcelable {
    companion object {
        fun fromApp(app: App): SharedLib {
            return SharedLib(
                app.packageName,
                app.versionCode,
                app.fileList.filterNot { it.url.isBlank() }
            )
        }
    }
}
