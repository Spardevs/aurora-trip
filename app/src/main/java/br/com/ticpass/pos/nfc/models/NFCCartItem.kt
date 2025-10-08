package br.com.ticpass.pos.nfc.models

/**
 * Represents a single item in the NFC cart
 * @param id Product identifier (0-65535)
 * @param count Item quantity (0-255)
 */
data class NFCCartItem(
    val id: UShort,  // 2 bytes (0-65535)
    val count: UByte     // 1 byte (0-255)
) {
    /**
     * Converts the cart item to a 3-byte array for NFC storage
     * Format: [productId_low, productId_high, quantity]
     */
    fun toByteArray(): ByteArray {
        return byteArrayOf(
            (id.toInt() and 0xFF).toByte(),           // Product ID low byte
            ((id.toInt() shr 8) and 0xFF).toByte(),   // Product ID high byte
            count.toByte()                                 // Quantity
        )
    }
    
    companion object {
        const val SIZE_BYTES = 3
        
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
            
            return NFCCartItem(productId, quantity)
        }
    }
}
