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
import android.util.Log
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap

object ShortcutManagerUtil {

    private const val TAG = "ShortcutManagerUtil"

    fun canPinShortcut(context: Context, packageName: String): Boolean {
        return ShortcutManagerCompat.isRequestPinShortcutSupported(context) &&
                context.packageManager.getLaunchIntentForPackage(packageName) != null
    }

    fun requestPinShortcut(context: Context, packageName: String) {
        val packageManager = context.packageManager
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val shortcutInfo = ShortcutInfoCompat.Builder(context, packageName)
                .setShortLabel(appInfo.loadLabel(packageManager))
                .setIcon(
                    IconCompat.createWithBitmap(appInfo.loadIcon(packageManager).toBitmap())
                )
                .setIntent(launchIntent)
                .build()

            ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to request shortcut pin!", exception)
        }
    }
}
