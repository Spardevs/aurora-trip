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

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.aurora.gplayapi.data.models.PlayFile
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@ProvidedTypeConverter
class DownloadConverter @Inject constructor(private val json: Json) {

    @TypeConverter
    fun toSharedLibList(string: String): List<SharedLib> {
        return json.decodeFromString<List<SharedLib>>(string)
    }

    @TypeConverter
    fun fromSharedLibList(list: List<SharedLib>): String {
        return json.encodeToString(list)
    }

    @TypeConverter
    fun toGPlayFileList(string: String): List<PlayFile> {
        return json.decodeFromString<List<PlayFile>>(string)
    }

    @TypeConverter
    fun fromGPlayFileList(list: List<PlayFile>): String {
        return json.encodeToString(list)
    }
}
