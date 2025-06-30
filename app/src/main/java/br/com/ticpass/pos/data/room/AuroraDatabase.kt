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

package br.com.ticpass.pos.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import br.com.ticpass.pos.data.room.download.Download
import br.com.ticpass.pos.data.room.download.DownloadConverter
import br.com.ticpass.pos.data.room.download.DownloadDao
import br.com.ticpass.pos.data.room.favourite.Favourite
import br.com.ticpass.pos.data.room.favourite.FavouriteDao
import br.com.ticpass.pos.data.room.update.Update
import br.com.ticpass.pos.data.room.update.UpdateDao

@Database(
    entities = [Download::class, Favourite::class, Update::class],
    version = 5,
    exportSchema = true
)
@TypeConverters(DownloadConverter::class)
abstract class AuroraDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun favouriteDao(): FavouriteDao
    abstract fun updateDao(): UpdateDao
}
