package br.com.ticpass.pos.core.util

class MoneyUtils {
    companion object {
        private const val CONVERSION_FACTOR = 100L

        fun convertLongToBrazilianCurrencyString(amount: Long): String {
            val decimalAmount = amount / CONVERSION_FACTOR.toDouble()
            return String.format("%.2f", decimalAmount)
        }

        fun convertDoubleToBrazilianCurrencyString(amount: Double): String {
            val decimalAmount = amount / CONVERSION_FACTOR.toDouble()
            return String.format("%.2f", decimalAmount)
        }
    }
}