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

package br.com.ticpass.pos.data.providers

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import br.com.ticpass.pos.util.PackageUtil.getPackageInfo

class NativeGsfVersionProvider(context: Context, isExport: Boolean = false) {
    private val GOOGLE_SERVICES_PACKAGE_ID = "com.google.android.gms"
    private val GOOGLE_VENDING_PACKAGE_ID = "com.android.vending"

    // Preferred defaults, not any specific reason they just work fine.
    var gsfVersionCode = 203019037L
    var vendingVersionCode = 82151710L
    var vendingVersionString = "21.5.17-21 [0] [PR] 326734551"

    init {
        try {
            if (isExport) {
                getPackageInfo(context, GOOGLE_SERVICES_PACKAGE_ID).let {
                    gsfVersionCode = PackageInfoCompat.getLongVersionCode(it)
                }

                getPackageInfo(context, GOOGLE_VENDING_PACKAGE_ID).let {
                    vendingVersionCode = PackageInfoCompat.getLongVersionCode(it)
                    vendingVersionString = it.versionName ?: vendingVersionString
                }
            }
        } catch (_: PackageManager.NameNotFoundException) {
        }
    }
}
