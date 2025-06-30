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

package br.com.ticpass.pos.data.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller.EXTRA_UNARCHIVE_PACKAGE_NAME
import android.util.Log
import androidx.core.content.getSystemService
import br.com.ticpass.extensions.isVAndAbove
import com.aurora.gplayapi.helpers.AppDetailsHelper
import br.com.ticpass.pos.AuroraApp
import br.com.ticpass.pos.data.helper.DownloadHelper
import br.com.ticpass.pos.data.providers.AccountProvider
import br.com.ticpass.pos.util.NotificationUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Triggers re-install/unarchive of a previously archived app on Android 15+ devices.
 */
@AndroidEntryPoint
class UnarchivePackageReceiver: BroadcastReceiver() {

    private val TAG = UnarchivePackageReceiver::class.java.simpleName

    @Inject
    lateinit var appDetailsHelper: AppDetailsHelper

    @Inject
    lateinit var downloadHelper: DownloadHelper

    override fun onReceive(context: Context?, intent: Intent?) {
        if (isVAndAbove && context != null && intent?.action == Intent.ACTION_UNARCHIVE_PACKAGE) {
            val packageName = intent.getStringExtra(EXTRA_UNARCHIVE_PACKAGE_NAME)!!
            Log.i(TAG, "Received request to unarchive $packageName")

            AuroraApp.scope.launch(Dispatchers.IO) {
                if (!AccountProvider.isLoggedIn(context)) {
                    Log.e(TAG, "Failed to authenticate user!")
                    with(context.getSystemService<NotificationManager>()!!) {
                        notify(
                            packageName.hashCode(),
                            NotificationUtil.getUnarchiveAuthNotification(context, packageName)
                        )
                    }
                    return@launch
                }

                val app = appDetailsHelper.getAppByPackageName(packageName)
                downloadHelper.enqueueApp(app)
            }
        }
    }
}
