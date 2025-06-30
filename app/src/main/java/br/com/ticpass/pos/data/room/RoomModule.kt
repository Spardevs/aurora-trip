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

import android.content.Context
import androidx.room.Room
import br.com.ticpass.pos.data.room.MigrationHelper.MIGRATION_1_2
import br.com.ticpass.pos.data.room.MigrationHelper.MIGRATION_2_3
import br.com.ticpass.pos.data.room.MigrationHelper.MIGRATION_3_4
import br.com.ticpass.pos.data.room.MigrationHelper.MIGRATION_4_5
import br.com.ticpass.pos.data.room.download.DownloadConverter
import br.com.ticpass.pos.data.room.download.DownloadDao
import br.com.ticpass.pos.data.room.favourite.FavouriteDao
import br.com.ticpass.pos.data.room.update.UpdateDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {

    private const val DATABASE = "aurora_database"

    @Singleton
    @Provides
    fun providesRoomInstance(
        @ApplicationContext context: Context,
        downloadConverter: DownloadConverter
    ): AuroraDatabase {
        return Room.databaseBuilder(context, AuroraDatabase::class.java, DATABASE)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .addTypeConverter(downloadConverter)
            .build()
    }

    @Provides
    fun providesDownloadDao(auroraDatabase: AuroraDatabase): DownloadDao {
        return auroraDatabase.downloadDao()
    }

    @Provides
    fun providesFavouriteDao(auroraDatabase: AuroraDatabase): FavouriteDao {
        return auroraDatabase.favouriteDao()
    }

    @Provides
    fun providesUpdateDao(auroraDatabase: AuroraDatabase): UpdateDao {
        return auroraDatabase.updateDao()
    }
}
