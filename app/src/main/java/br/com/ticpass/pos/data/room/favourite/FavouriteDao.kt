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

package br.com.ticpass.pos.data.room.favourite

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favourite: Favourite)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(favourites: List<Favourite>)

    @Query("SELECT * FROM favourite")
    fun favourites(): Flow<List<Favourite>>

    @Query("SELECT EXISTS(SELECT 1 FROM favourite WHERE packageName = :packageName)")
    suspend fun isFavourite(packageName: String): Boolean

    @Query("DELETE FROM favourite WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("DELETE FROM favourite")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM favourite")
    suspend fun count(): Int
}
