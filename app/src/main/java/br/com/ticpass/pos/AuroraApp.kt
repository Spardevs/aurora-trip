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

package br.com.ticpass.pos

import android.app.Application
import android.content.Context
import android.util.Log.DEBUG
import android.util.Log.INFO
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import br.com.ticpass.extensions.isPAndAbove
import br.com.ticpass.extensions.setAppTheme
import br.com.ticpass.pos.data.event.EventFlow
import br.com.ticpass.pos.data.helper.DownloadHelper
import br.com.ticpass.pos.data.helper.UpdateHelper
import br.com.ticpass.pos.data.receiver.PackageManagerReceiver
import br.com.ticpass.pos.util.CommonUtil
import br.com.ticpass.pos.util.NotificationUtil
import br.com.ticpass.pos.util.PackageUtil
import br.com.ticpass.pos.util.Preferences
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.MainScope
import okhttp3.OkHttpClient
import org.lsposed.hiddenapibypass.HiddenApiBypass
import javax.inject.Inject

//@HiltAndroidApp
class AuroraApp : Application(), Configuration.Provider, SingletonImageLoader.Factory {

    @Inject
    lateinit var okHttpClient: OkHttpClient

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var downloadHelper: DownloadHelper

    @Inject
    lateinit var updateHelper: UpdateHelper

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) DEBUG else INFO)
            .setWorkerFactory(workerFactory)
            .build()

    companion object {
        var scope = MainScope()
            private set

        val enqueuedInstalls: MutableSet<String> = mutableSetOf()
        val events = EventFlow()
    }

    override fun onCreate() {
        super.onCreate()
        // Set the app theme
        val themeStyle = Preferences.getInteger(this, Preferences.PREFERENCE_THEME_STYLE)
        setAppTheme(themeStyle)

        // Apply dynamic colors to activities
        DynamicColors.applyToActivitiesIfAvailable(this)

        // Required for Shizuku installer
        if (isPAndAbove) HiddenApiBypass.addHiddenApiExemptions("I", "L")

        //Create Notification Channels
        NotificationUtil.createNotificationChannel(this)

        // Initialize Download and Update helpers to observe and trigger downloads
        downloadHelper.init()
        updateHelper.init()

        //Register broadcast receiver for package install/uninstall
        ContextCompat.registerReceiver(
            this,
            object : PackageManagerReceiver() {},
            PackageUtil.getFilter(),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        CommonUtil.cleanupInstallationSessions(applicationContext)
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader(this).newBuilder()
            .crossfade(true)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = okHttpClient)) }
            .build()
    }
}
