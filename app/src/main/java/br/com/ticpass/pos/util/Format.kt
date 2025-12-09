package br.com.ticpass.pos.util

import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatPrice(
    price: Long,
    showCurrencyPrefix: Boolean = true
): String {
    val localeBrazil = Locale("pt", "BR")
    val currencyFormatter = NumberFormat.getCurrencyInstance(localeBrazil)

    val priceInBigDecimal = BigDecimal(price).movePointLeft(2)
    val formattedPrice = currencyFormatter.format(priceInBigDecimal)

    val priceString = if (showCurrencyPrefix) {
        formattedPrice
    } else {
        formattedPrice.substring(currencyFormatter.currency.symbol.length)
    }

    return if (price < 0) {
        priceString.replaceFirst("-", "- ")
    } else {
        priceString
    }
}

fun getDateFromString(dateString: String): Date {
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    return format.parse(dateString)
}