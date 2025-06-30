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

package br.com.ticpass.extensions

import android.annotation.SuppressLint
import android.os.Build
import java.util.Locale

val isMAndAbove: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

val isNAndAbove: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

val isOAndAbove: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

val isPAndAbove: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

val isQAndAbove: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

val isRAndAbove: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

val isSAndAbove: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

val isTAndAbove: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

val isUAndAbove: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

val isVAndAbove: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM

val isMIUI: Boolean
    get() = !getSystemProperty("ro.miui.ui.version.name").isNullOrBlank()

val isHuawei: Boolean
    get() = Build.MANUFACTURER.lowercase(Locale.getDefault()).contains("huawei")
            || Build.HARDWARE.lowercase(Locale.getDefault()).contains("kirin")
            || Build.HARDWARE.lowercase(Locale.getDefault()).contains("hi3")

@get:SuppressLint("PrivateApi")
val isMiuiOptimizationDisabled: Boolean
    get() {
        return if ("0" == getSystemProperty("persist.sys.miui_optimization")) {
            true
        } else try {
            Class.forName("android.miui.AppOpsUtils")
                .getDeclaredMethod("isXOptMode")
                .invoke(null) as Boolean
        } catch (_: java.lang.Exception) {
            false
        }
    }

@SuppressLint("PrivateApi")
private fun getSystemProperty(key: String): String? {
    return try {
        Class.forName("android.os.SystemProperties")
            .getDeclaredMethod("get", String::class.java)
            .invoke(null, key) as String
    } catch (e: Exception) {
        null
    }
}
