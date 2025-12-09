package br.com.ticpass.pos.core.nfc.models

import br.com.ticpass.Constants

/**
 * Represents a single item in the NFC cart with price locking
 * @param id Product identifier (0-65535)
 * @param count Item quantity (0-255)
 * @param price Price per unit in smallest units based on CONVERSION_FACTOR (0-4,294,967,295)
 */
data class NFCCartItem(
    val id: UShort,           // 2 bytes (0-65535)
    val count: UByte,         // 1 byte (0-255)
    val price: UInt           // 4 bytes (0-4,294,967,295)
) {
    /**
     * Returns price formatted as currency string using CONVERSION_FACTOR
     */
    fun formattedPrice(): String {
        val factor = Constants.CONVERSION_FACTOR
        val whole = price.toLong() / factor
        val fraction = price.toLong() % factor
        val fractionDigits = factor.toString().length - 1
        return "$${whole}.${fraction.toString().padStart(fractionDigits, '0')}"
    }
    
    /**
     * Returns total price (price * count) formatted as currency string
     */
    fun formattedTotalPrice(): String {
        val factor = Constants.CONVERSION_FACTOR
        val total = price.toLong() * count.toInt()
        val whole = total / factor
        val fraction = total % factor
        val fractionDigits = factor.toString().length - 1
        return "$${whole}.${fraction.toString().padStart(fractionDigits, '0')}"
    }
    /**
     * Converts the cart item to a 7-byte array for NFC storage
     * Format: [productId_low, productId_high, quantity, price_byte0, price_byte1, price_byte2, price_byte3]
     */
    fun toByteArray(): ByteArray {
        return byteArrayOf(
            (id.toInt() and 0xFF).toByte(),                      // Product ID low byte
            ((id.toInt() shr 8) and 0xFF).toByte(),              // Product ID high byte
            count.toByte(),                                       // Quantity
            (price.toInt() and 0xFF).toByte(),            // Price byte 0 (lowest)
            ((price.toInt() shr 8) and 0xFF).toByte(),    // Price byte 1
            ((price.toInt() shr 16) and 0xFF).toByte(),   // Price byte 2
            ((price.toInt() shr 24) and 0xFF).toByte()    // Price byte 3 (highest)
        )
    }
    
    companion object {
        const val SIZE_BYTES = 7
        
        /**
         * Creates an NFCCartItem from a byte array
         * @param data The byte array containing cart item data
         * @param offset The offset in the array where the item data starts
         * @return NFCCartItem or null if data is invalid
         */
        fun fromByteArray(data: ByteArray, offset: Int = 0): NFCCartItem? {
            if (data.size < offset + SIZE_BYTES) return null
            
            val productId = ((data[offset].toInt() and 0xFF) or 
                           ((data[offset + 1].toInt() and 0xFF) shl 8)).toUShort()
            val quantity = (data[offset + 2].toInt() and 0xFF).toUByte()
            val price = ((data[offset + 3].toInt() and 0xFF) or
                              ((data[offset + 4].toInt() and 0xFF) shl 8) or
                              ((data[offset + 5].toInt() and 0xFF) shl 16) or
                              ((data[offset + 6].toInt() and 0xFF) shl 24)).toUInt()
            
            return NFCCartItem(productId, quantity, price)
        }
    }
}
