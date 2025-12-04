package br.com.ticpass.pos.core.util

import br.com.ticpass.Constants

class NumericConversionUtils {
    companion object {

        fun convertLongToBrCurrencyString(amount: Long): String {
            val decimalAmount = amount / Constants.CONVERSION_FACTOR.toDouble()
            return String.format("%.2f", decimalAmount)
        }

        /**
         * Converte um Long para String de porcentagem.
         * Exemplo: Se o fator for 100 e a entrada for 1550, retorna "15.50%"
         */
        fun convertLongToPercentString(amount: Long): String {
            val decimalAmount = amount / Constants.CONVERSION_FACTOR.toDouble()
            return String.format("%.2f%%", decimalAmount)
        }
    }
}