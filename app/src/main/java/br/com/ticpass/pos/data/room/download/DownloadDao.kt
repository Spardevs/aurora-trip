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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aurora.gplayapi.data.models.PlayFile
import br.com.ticpass.pos.data.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: Download)

    @Query("UPDATE download SET downloadStatus=:downloadStatus WHERE packageName=:packageName")
    suspend fun updateStatus(packageName: String, downloadStatus: DownloadStatus)

    @Query("UPDATE download SET fileList=:fileList WHERE packageName=:packageName")
    suspend fun updateFiles(packageName: String, fileList: List<PlayFile>)

    @Query("UPDATE download SET sharedLibs=:sharedLibs WHERE packageName=:packageName")
    suspend fun updateSharedLibs(packageName: String, sharedLibs: List<SharedLib>)

    @Query(
        """
        UPDATE download
        SET progress=:progress, speed=:speed, timeRemaining=:timeRemaining
        WHERE packageName=:packageName
        """
    )
    suspend fun updateProgress(
        packageName: String,
        progress: Int,
        speed: Long,
        timeRemaining: Long
    )

    @Query("SELECT * FROM download")
    fun downloads(): Flow<List<Download>>

    @Query("SELECT * FROM download WHERE packageName = :packageName")
    suspend fun getDownload(packageName: String): Download

    @Query("DELETE FROM download WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("DELETE FROM download")
    suspend fun deleteAll()
}
