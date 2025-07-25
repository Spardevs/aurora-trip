package br.com.ticpass.pos.queue.payment.processors

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for generating PIX payment strings
 * Follows the Brazilian PIX standard for static and dynamic QR codes
 */
object PixStringGenerator {
    // PIX constants
    private const val ID_PAYLOAD_FORMAT_INDICATOR = "00"
    private const val ID_MERCHANT_ACCOUNT_INFO = "26"
    private const val ID_MERCHANT_CATEGORY_CODE = "52"
    private const val ID_TRANSACTION_CURRENCY = "53"
    private const val ID_TRANSACTION_AMOUNT = "54"
    private const val ID_COUNTRY_CODE = "58"
    private const val ID_MERCHANT_NAME = "59"
    private const val ID_MERCHANT_CITY = "60"
    private const val ID_ADDITIONAL_DATA_FIELD = "62"
    private const val ID_CRC16 = "63"
    
    private const val ID_PIX_GUI = "00"
    private const val ID_PIX_KEY = "01"
    private const val ID_PIX_INFO_ADDITIONAL = "02"
    private const val ID_PIX_MERCHANT_ACCOUNT_INFORMATION = "26"
    
    private const val ID_REFERENCE_LABEL = "05"
    private const val ID_EXPIRATION = "04"
    
    /**
     * Generate a PIX string for payment
     *
     * @param pixKey The PIX key (CPF, CNPJ, email, phone, or random key)
     * @param amount The payment amount
     * @param expirationMinutes The expiration time in minutes (optional)
     * @param merchantName The merchant name (default: "TICPASS")
     * @param merchantCity The merchant city (default: "SAO PAULO")
     * @param reference Additional reference information (optional)
     * @return The generated PIX string
     */
    fun generatePixString(
        pixKey: String,
        amount: Int,
        expirationMinutes: Int? = null,
        merchantName: String = "TICPASS",
        merchantCity: String = "SAO PAULO",
        reference: String? = null
    ): String {
        val pixGuiValue = "br.gov.bcb.pix"
        val merchantCategoryCode = "0000"
        val transactionCurrency = "986" // BRL
        val countryCode = "BR"
        
        // Format amount with 2 decimal places
        val amountFormatted = DecimalFormat("0.00").format(amount).replace(",", ".")
        
        // Build the PIX string
        val sb = StringBuilder()
        
        // Payload format indicator
        sb.append(formatField(ID_PAYLOAD_FORMAT_INDICATOR, "01"))
        
        // Merchant account information
        val merchantAccountInfo = StringBuilder()
        merchantAccountInfo.append(formatField(ID_PIX_GUI, pixGuiValue))
        merchantAccountInfo.append(formatField(ID_PIX_KEY, pixKey))
        
        // Add additional info if reference is provided
        if (reference != null) {
            merchantAccountInfo.append(formatField(ID_PIX_INFO_ADDITIONAL, reference))
        }
        
        sb.append(formatField(ID_MERCHANT_ACCOUNT_INFO, merchantAccountInfo.toString()))
        
        // Merchant category code
        sb.append(formatField(ID_MERCHANT_CATEGORY_CODE, merchantCategoryCode))
        
        // Transaction currency
        sb.append(formatField(ID_TRANSACTION_CURRENCY, transactionCurrency))
        
        // Transaction amount
        sb.append(formatField(ID_TRANSACTION_AMOUNT, amountFormatted))
        
        // Country code
        sb.append(formatField(ID_COUNTRY_CODE, countryCode))
        
        // Merchant name (limited to 25 characters)
        sb.append(formatField(ID_MERCHANT_NAME, merchantName.take(25)))
        
        // Merchant city (limited to 15 characters)
        sb.append(formatField(ID_MERCHANT_CITY, merchantCity.take(15)))
        
        // Additional data field
        val additionalData = StringBuilder()
        
        // Add expiration if provided
        if (expirationMinutes != null) {
            val expirationDate = Date(System.currentTimeMillis() + expirationMinutes * 60 * 1000)
            val expirationFormatted = SimpleDateFormat("yyyyMMddHHmm", Locale.US).format(expirationDate)
            additionalData.append(formatField(ID_EXPIRATION, expirationFormatted))
        }
        
        // Add reference if provided
        if (reference != null) {
            additionalData.append(formatField(ID_REFERENCE_LABEL, reference.take(25)))
        }
        
        if (additionalData.isNotEmpty()) {
            sb.append(formatField(ID_ADDITIONAL_DATA_FIELD, additionalData.toString()))
        }
        
        // Add CRC16 placeholder
        sb.append(ID_CRC16).append("04")
        
        // Calculate and append CRC16
        val crc16 = calculateCRC16(sb.toString())
        sb.append(crc16)
        
        return sb.toString()
    }
    
    /**
     * Format a field with its ID and length
     */
    private fun formatField(id: String, value: String): String {
        return id + String.format("%02d", value.length) + value
    }
    
    /**
     * Calculate CRC16 for the PIX string
     */
    private fun calculateCRC16(str: String): String {
        val polynomial = 0x1021 // CRC16 CCITT polynomial
        var crc = 0xFFFF // Initial value
        
        for (i in str.indices) {
            crc = crc xor (str[i].code shl 8)
            
            for (j in 0 until 8) {
                crc = if (crc and 0x8000 != 0) {
                    (crc shl 1) xor polynomial
                } else {
                    crc shl 1
                }
                crc = crc and 0xFFFF // Keep 16 bits
            }
        }
        
        return String.format("%04X", crc)
    }
}
