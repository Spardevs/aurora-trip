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

import android.content.pm.PackageManager
import br.com.ticpass.pos.BuildConfig

/**
 * Gets the name of package responsible for installing/updating given package
 */
fun PackageManager.getUpdateOwnerPackageNameCompat(packageName: String): String? {
    // Self-updates can be managed by ourselves
    if (packageName == BuildConfig.APPLICATION_ID) return BuildConfig.APPLICATION_ID

    return when {
        isUAndAbove -> {
            // If update ownership is null, we can still silently update it if we installed it
            val installSourceInfo = getInstallSourceInfo(packageName)
            installSourceInfo.updateOwnerPackageName ?: installSourceInfo.installingPackageName
        }

        isRAndAbove -> {
            val installSourceInfo = getInstallSourceInfo(packageName)
            installSourceInfo.installingPackageName
        }

        else -> @Suppress("DEPRECATION") getInstallerPackageName(packageName)
    }
}
