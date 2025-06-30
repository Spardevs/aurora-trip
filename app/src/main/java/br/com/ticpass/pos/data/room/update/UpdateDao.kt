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

package br.com.ticpass.pos.data.room.update

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface UpdateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(updates: List<Update>)

    @Query("SELECT * FROM `update` ORDER BY displayName ASC")
    fun updates(): Flow<List<Update>>

    @Query("DELETE FROM `update` WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("DELETE FROM `update`")
    suspend fun deleteAll()

    @Transaction
    suspend fun insertUpdates(updates: List<Update>) {
        deleteAll()
        insertAll(updates)
    }
}
