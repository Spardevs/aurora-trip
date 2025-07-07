package br.com.ticpass.pos.data.room.entity

import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Entity(tableName = "events" )
data class EventEntity(
    @PrimaryKey val id: String,
    var name: String,
    val logo: String,
    val pin: String,
    val details: String,
    val dateStart: String,
    val dateEnd: String,
    var printingPriceEnabled: Boolean,
    var ticketsPrintingGrouped: Boolean,

    @ColumnInfo(defaultValue = "false")
    var isSelected: Boolean = false,

    var mode: String,

    var hasProducts: Boolean,
    val isCreditEnabled: Boolean,
    val isDebitEnabled: Boolean,
    val isPIXEnabled: Boolean,

    @ColumnInfo(defaultValue = "default")
    var ticketFormat: String = "default",

    @ColumnInfo(defaultValue = "true")
    val isVREnabled: Boolean,
    val isLnBTCEnabled: Boolean,
    val isCashEnabled: Boolean,
    val isAcquirerPaymentEnabled: Boolean,
    val isMultiPaymentEnabled: Boolean,
) {

    fun getFormattedStartDate(): String {
        val brazilLocale = Locale("pt", "BR")
        val saoPauloTimeZone = TimeZone.getTimeZone("America/Sao_Paulo")
        val startDate = SimpleDateFormat("d MMM às HH:mm", brazilLocale)
            .apply { timeZone = saoPauloTimeZone }
            .format(parseDate(dateStart))

        return startDate
    }

    fun getFormattedDateRange(): String {
        val brazilLocale = Locale("pt", "BR")
        val saoPauloTimeZone = TimeZone.getTimeZone("America/Sao_Paulo")

        val dateFormat = SimpleDateFormat("HH:mm", brazilLocale)
            .apply { timeZone = saoPauloTimeZone }

        val startDate = SimpleDateFormat("d MMM", brazilLocale)
            .apply { timeZone = saoPauloTimeZone }
            .format(parseDate(dateStart))

        val endDate = SimpleDateFormat("d MMM", brazilLocale)
            .apply { timeZone = saoPauloTimeZone }
            .format(parseDate(dateEnd))

        val sameDay = startDate == endDate

        if (sameDay) {
            return "$startDate ・ ${dateFormat.format(parseDate(dateStart))} às ${dateFormat.format(parseDate(dateEnd))}"
        }

        return "$startDate às ${dateFormat.format(parseDate(dateStart))} ・ $endDate às ${dateFormat.format(parseDate(dateEnd))}"
    }

    private fun parseDate(dateString: String): Date {
        val brazilLocale = Locale("pt", "BR")
        val saoPauloTimeZone = TimeZone.getTimeZone("America/Sao_Paulo")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", brazilLocale)
            .apply { timeZone = saoPauloTimeZone }

        return dateFormat.parse(dateString)
    }

    override fun toString() = name
}
