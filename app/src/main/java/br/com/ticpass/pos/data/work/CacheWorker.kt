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

package br.com.ticpass.pos.data.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy.KEEP
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import br.com.ticpass.pos.util.PathUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.util.Calendar
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MINUTES
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * A periodic worker to automatically clear the old downloads cache periodically.
 */
@HiltWorker
class CacheWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "CleanCacheWorker"
        private const val CLEAN_CACHE_WORKER = "CLEAN_CACHE_WORKER"

        /**
         * Schedules the automated cache cleanup
         * @param context Current [Context]
         * @see [CacheWorker]
         */
        fun scheduleAutomatedCacheCleanup(context: Context) {
            val periodicWorkRequest = PeriodicWorkRequestBuilder<CacheWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = HOURS,
                flexTimeInterval = 30,
                flexTimeIntervalUnit = MINUTES
            ).build()

            Log.i(TAG, "Scheduling periodic cache cleanup!")
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(CLEAN_CACHE_WORKER, KEEP, periodicWorkRequest)
        }
    }

    /**
     * Duration to cache files, defaults to 6 hours
     */
    private val cacheDuration = 6.toDuration(DurationUnit.HOURS)

    override suspend fun doWork(): Result {
        Log.i(TAG, "Cleaning cache")

        PathUtil.getOldDownloadDirectories(context).filter { it.exists() }.forEach { dir -> // Downloads
            Log.i(TAG, "Deleting old unused download directory: $dir")
            dir.deleteRecursively()
        }

        PathUtil.getDownloadDirectory(context).listFiles()?.forEach { download -> // com.example.app
            // Delete if the download directory is empty
            if (download.listFiles().isNullOrEmpty()) {
                Log.i(TAG, "Removing empty download directory for ${download.name}")
                download.deleteRecursively(); return@forEach
            }

            download.listFiles()!!.forEach { versionCode -> // 20240325
                if (versionCode.listFiles().isNullOrEmpty()) {
                    // Purge empty non-accessible directory
                    Log.i(TAG, "Removing empty directory for ${download.name}, ${versionCode.name}")
                    versionCode.deleteRecursively()
                } else {
                    versionCode.deleteIfOld()
                }
            }
        }

        return Result.success()
    }

    /**
     * Deletes the file if it's older than $[cacheDuration]
     */
    private fun File.deleteIfOld() {
        val elapsedTime = Calendar.getInstance().timeInMillis - this.lastModified()
        if (elapsedTime.toDuration(DurationUnit.HOURS) > cacheDuration) {
            Log.i(TAG, "Removing $this older than ${cacheDuration.inWholeHours} hours")
            this.deleteRecursively()
        }
    }
}
