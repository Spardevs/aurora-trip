package br.com.ticpass.pos.data.room.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

object Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: String): List<String> =
        gson.fromJson(value, object : TypeToken<List<String>>(){}.type)

    @TypeConverter
    fun toStringList(list: List<String>): String =
        gson.toJson(list)

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? =
        value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? =
        date?.time
}
