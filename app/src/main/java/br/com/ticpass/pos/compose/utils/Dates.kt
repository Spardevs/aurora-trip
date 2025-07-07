package br.com.ticpass.pos.compose.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun generateTimestamp(
    pattern: String = "EEEE, MMM - HH:mm",
    date: Date = Date()
): String {
    val format = SimpleDateFormat(pattern, Locale("pt", "BR"))
    val timeStamp = format.format(date)
    return timeStamp.lowercase()
}

fun getCurrentDateString(): String {
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    return format.format(Date())
}

fun getDateFromString(dateString: String): Date {
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    return format.parse(dateString)
}