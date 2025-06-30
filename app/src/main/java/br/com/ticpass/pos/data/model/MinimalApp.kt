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

package br.com.ticpass.pos.data.model

import android.graphics.Bitmap
import android.os.Parcelable
import com.aurora.gplayapi.data.models.App
import br.com.ticpass.pos.data.room.update.Update
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class MinimalApp(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val displayName: String,
    @IgnoredOnParcel
    val icon: Bitmap? = null
) : Parcelable {

    companion object {

        fun fromApp(app: App): MinimalApp {
            return MinimalApp(
                app.packageName,
                app.versionName,
                app.versionCode,
                app.displayName
            )
        }

        fun fromUpdate(update: Update): MinimalApp {
            return MinimalApp(
                update.packageName,
                update.versionName,
                update.versionCode,
                update.displayName
            )
        }

    }
}
